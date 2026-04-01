package dev.armanruhit.nexusvas.billing.service;

import dev.armanruhit.nexusvas.billing.domain.entity.BillingAccount;
import dev.armanruhit.nexusvas.billing.domain.entity.LedgerEntry;
import dev.armanruhit.nexusvas.billing.domain.entity.RevenueAccount;
import dev.armanruhit.nexusvas.billing.domain.repository.BillingAccountRepository;
import dev.armanruhit.nexusvas.billing.domain.repository.LedgerEntryRepository;
import dev.armanruhit.nexusvas.billing.domain.repository.RevenueAccountRepository;
import dev.armanruhit.nexusvas.billing.dto.ChargeRequest;
import dev.armanruhit.nexusvas.billing.dto.ChargeResult;
import dev.armanruhit.nexusvas.billing.exception.BillingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {

    private final BillingAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final RevenueAccountRepository revenueRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String IDEMPOTENCY_PREFIX = "billing_idem:";
    private static final String REVENUE_ACCOUNT_CODE = "SUBSCRIPTION_REVENUE";

    // ── Charge (idempotent) ───────────────────────────────────────────────────

    @Transactional
    public ChargeResult charge(ChargeRequest request) {
        String idempotencyKey = buildIdempotencyKey(request);

        // Check idempotency: if already processed, return cached result
        String cached = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
        if (cached != null) {
            log.debug("Duplicate charge request detected, returning cached result for key {}", idempotencyKey);
            return ChargeResult.duplicate(UUID.fromString(cached));
        }

        // Lock and load subscriber account
        BillingAccount subscriberAccount = accountRepository
            .findAndLockByTenantIdAndReferenceId(request.tenantId(), request.msisdn())
            .orElseThrow(() -> new BillingException("ACCOUNT_NOT_FOUND",
                "Billing account not found for msisdn: " + request.msisdn()));

        if (subscriberAccount.getStatus() != BillingAccount.AccountStatus.ACTIVE) {
            publishChargeFailed(request, "Account is not active");
            return ChargeResult.failed("Account is not active");
        }

        if (!subscriberAccount.hasSufficientBalance(request.amount())) {
            publishChargeFailed(request, "Insufficient balance");
            return ChargeResult.failed("Insufficient balance");
        }

        // Lock and load revenue account (credit side)
        RevenueAccount revenueAccount = revenueRepository
            .findAndLockByTenantIdAndCode(request.tenantId(), REVENUE_ACCOUNT_CODE)
            .orElseGet(() -> createRevenueAccount(request.tenantId(), request.currency()));

        UUID transactionId = UUID.randomUUID();

        // Double-entry: debit subscriber, credit revenue
        subscriberAccount.debit(request.amount());
        revenueAccount.credit(request.amount());

        accountRepository.save(subscriberAccount);
        revenueRepository.save(revenueAccount);

        // Write both ledger entries
        LedgerEntry debit = LedgerEntry.builder()
            .tenantId(request.tenantId())
            .transactionId(transactionId)
            .entryType(LedgerEntry.EntryType.DEBIT)
            .accountId(subscriberAccount.getId())
            .amount(request.amount())
            .currency(request.currency())
            .description("Subscription charge: " + request.productName())
            .referenceType("SUBSCRIPTION")
            .referenceId(request.subscriptionId().toString())
            .build();

        LedgerEntry credit = LedgerEntry.builder()
            .tenantId(request.tenantId())
            .transactionId(transactionId)
            .entryType(LedgerEntry.EntryType.CREDIT)
            .accountId(revenueAccount.getId())
            .amount(request.amount())
            .currency(request.currency())
            .description("Subscription revenue: " + request.productName())
            .referenceType("SUBSCRIPTION")
            .referenceId(request.subscriptionId().toString())
            .build();

        ledgerRepository.save(debit);
        ledgerRepository.save(credit);

        // Store idempotency key (24-hour TTL)
        redisTemplate.opsForValue().set(
            IDEMPOTENCY_PREFIX + idempotencyKey, transactionId.toString(), 24, TimeUnit.HOURS);

        // Publish ChargeSucceeded to Kafka
        publishChargeSucceeded(request, transactionId);

        log.info("Charge succeeded: tenantId={} msisdn={} amount={} txn={}",
            request.tenantId(), request.msisdn(), request.amount(), transactionId);

        return ChargeResult.success(transactionId);
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    @Transactional
    public ChargeResult refund(String tenantId, UUID originalTransactionId, String reason) {
        var entries = ledgerRepository.findByTransactionId(originalTransactionId);
        if (entries.isEmpty()) {
            throw new BillingException("TRANSACTION_NOT_FOUND",
                "Transaction not found: " + originalTransactionId);
        }

        UUID refundTransactionId = UUID.randomUUID();

        for (LedgerEntry entry : entries) {
            // Reverse: debit becomes credit and vice versa
            LedgerEntry reversal = LedgerEntry.builder()
                .tenantId(tenantId)
                .transactionId(refundTransactionId)
                .entryType(entry.getEntryType() == LedgerEntry.EntryType.DEBIT
                    ? LedgerEntry.EntryType.CREDIT : LedgerEntry.EntryType.DEBIT)
                .accountId(entry.getAccountId())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .description("Refund for transaction: " + originalTransactionId + " - " + reason)
                .referenceType("REFUND")
                .referenceId(originalTransactionId.toString())
                .build();
            ledgerRepository.save(reversal);

            // Update account balances
            accountRepository.findById(entry.getAccountId()).ifPresent(account -> {
                if (entry.getEntryType() == LedgerEntry.EntryType.DEBIT) {
                    account.credit(entry.getAmount()); // reverse the debit = credit back subscriber
                } else {
                    account.debit(entry.getAmount()); // reverse the credit = debit revenue account
                }
                accountRepository.save(account);
            });
        }

        // Publish RefundIssued event
        kafkaTemplate.send("billing-events", tenantId, Map.of(
            "eventType", "RefundIssued",
            "tenantId", tenantId,
            "originalTransactionId", originalTransactionId.toString(),
            "refundTransactionId", refundTransactionId.toString(),
            "reason", reason,
            "timestamp", Instant.now().toString()
        ));

        return ChargeResult.success(refundTransactionId);
    }

    // ── Account Management ────────────────────────────────────────────────────

    @Transactional
    public BillingAccount getOrCreateSubscriberAccount(
            String tenantId, String msisdn, String currency) {
        return accountRepository.findByTenantIdAndReferenceId(tenantId, msisdn)
            .orElseGet(() -> {
                BillingAccount account = BillingAccount.builder()
                    .tenantId(tenantId)
                    .accountType(BillingAccount.AccountType.SUBSCRIBER)
                    .referenceId(msisdn)
                    .currency(currency)
                    .balance(BigDecimal.ZERO)
                    .status(BillingAccount.AccountStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build();
                return accountRepository.save(account);
            });
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private String buildIdempotencyKey(ChargeRequest request) {
        return request.tenantId() + ":" + request.msisdn() + ":" +
               request.subscriptionId() + ":" + request.billingCycleDate();
    }

    private void publishChargeSucceeded(ChargeRequest request, UUID transactionId) {
        kafkaTemplate.send("billing-events", request.tenantId(), Map.of(
            "eventType", "ChargeSucceeded",
            "tenantId", request.tenantId(),
            "subscriptionId", request.subscriptionId().toString(),
            "msisdn", request.msisdn(),
            "amount", request.amount().toString(),
            "currency", request.currency(),
            "transactionId", transactionId.toString(),
            "timestamp", Instant.now().toString()
        ));
    }

    private void publishChargeFailed(ChargeRequest request, String reason) {
        kafkaTemplate.send("billing-events", request.tenantId(), Map.of(
            "eventType", "ChargeFailed",
            "tenantId", request.tenantId(),
            "subscriptionId", request.subscriptionId().toString(),
            "msisdn", request.msisdn(),
            "failureReason", reason,
            "timestamp", Instant.now().toString()
        ));
    }

    private RevenueAccount createRevenueAccount(String tenantId, String currency) {
        RevenueAccount account = RevenueAccount.builder()
            .tenantId(tenantId)
            .accountCode(REVENUE_ACCOUNT_CODE)
            .accountName("Subscription Revenue")
            .accountCategory(RevenueAccount.AccountCategory.REVENUE)
            .balance(BigDecimal.ZERO)
            .currency(currency)
            .createdAt(Instant.now())
            .build();
        return revenueRepository.save(account);
    }
}

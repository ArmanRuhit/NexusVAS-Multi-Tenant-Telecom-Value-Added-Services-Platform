package dev.armanruhit.nexusvas.billing.controller;

import dev.armanruhit.nexusvas.billing.domain.entity.BillingAccount;
import dev.armanruhit.nexusvas.billing.domain.entity.LedgerEntry;
import dev.armanruhit.nexusvas.billing.domain.repository.BillingAccountRepository;
import dev.armanruhit.nexusvas.billing.domain.repository.LedgerEntryRepository;
import dev.armanruhit.nexusvas.billing.dto.ChargeResult;
import dev.armanruhit.nexusvas.billing.service.BillingService;
import dev.armanruhit.nexusvas.common_lib.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final BillingAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;

    @GetMapping("/accounts/{msisdn}")
    public ResponseEntity<ApiResponse<BillingAccount>> getAccount(
            @PathVariable String msisdn,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return accountRepository.findByTenantIdAndReferenceId(tenantId, msisdn)
            .map(a -> ResponseEntity.ok(ApiResponse.success(a)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts/{msisdn}/ledger")
    public ResponseEntity<ApiResponse<Page<LedgerEntry>>> getLedger(
            @PathVariable String msisdn,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return accountRepository.findByTenantIdAndReferenceId(tenantId, msisdn)
            .map(account -> {
                Page<LedgerEntry> entries = ledgerRepository.findByTenantIdAndAccountId(
                    tenantId, account.getId(), pageable);
                return ResponseEntity.ok(ApiResponse.success(entries));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refund/{transactionId}")
    public ResponseEntity<ApiResponse<ChargeResult>> refund(
            @PathVariable UUID transactionId,
            @RequestParam(defaultValue = "Operator issued refund") String reason,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        ChargeResult result = billingService.refund(tenantId, transactionId, reason);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/accounts/{msisdn}/credit")
    public ResponseEntity<ApiResponse<Map<String, String>>> creditAccount(
            @PathVariable String msisdn,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam String currency,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        BillingAccount account = billingService.getOrCreateSubscriberAccount(tenantId, msisdn, currency);
        account.credit(amount);
        accountRepository.save(account);
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("accountId", account.getId().toString(), "newBalance", account.getBalance().toString())));
    }
}

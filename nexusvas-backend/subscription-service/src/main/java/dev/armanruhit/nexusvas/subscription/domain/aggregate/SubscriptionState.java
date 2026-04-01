package dev.armanruhit.nexusvas.subscription.domain.aggregate;

import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.BillingCycle;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.SubscriptionStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mutable state rebuilt by replaying events for a subscription aggregate.
 */
@Getter
public class SubscriptionState {

    private UUID subscriptionId;
    private String tenantId;
    private String msisdn;
    private UUID productId;
    private String productName;
    private SubscriptionStatus status;
    private BillingCycle billingCycle;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private int renewalCount;
    private Instant lastBilledAt;
    private Instant nextBillingAt;
    private int version;

    // Default constructor for a new/empty aggregate
    public SubscriptionState() {
        this.status = SubscriptionStatus.PENDING;
        this.version = 0;
        this.renewalCount = 0;
    }

    public boolean isNew() {
        return version == 0;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isCancelled() {
        return status == SubscriptionStatus.CANCELLED;
    }

    // ── Event Appliers ────────────────────────────────────────────────────────

    public void apply(SubscriptionCreatedPayload p) {
        this.subscriptionId = p.subscriptionId();
        this.tenantId = p.tenantId();
        this.msisdn = p.msisdn();
        this.productId = p.productId();
        this.productName = p.productName();
        this.billingCycle = p.billingCycle();
        this.priceAmount = p.priceAmount();
        this.priceCurrency = p.priceCurrency();
        this.startedAt = p.startedAt();
        this.nextBillingAt = p.nextBillingAt();
        this.status = SubscriptionStatus.PENDING; // pending until billing confirms
    }

    public void apply(SubscriptionActivatedPayload p) {
        this.status = SubscriptionStatus.ACTIVE;
        this.lastBilledAt = p.billedAt();
        this.nextBillingAt = p.nextBillingAt();
    }

    public void apply(SubscriptionRenewedPayload p) {
        this.renewalCount++;
        this.lastBilledAt = p.billedAt();
        this.nextBillingAt = p.nextBillingAt();
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void apply(SubscriptionCancelledPayload p) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = p.cancelledAt();
        this.cancellationReason = p.reason();
    }

    public void apply(SubscriptionExpiredPayload p) {
        this.status = SubscriptionStatus.EXPIRED;
        this.expiresAt = p.expiredAt();
    }

    public void apply(SubscriptionSuspendedPayload p) {
        this.status = SubscriptionStatus.SUSPENDED;
    }

    public void incrementVersion() {
        this.version++;
    }

    // ── Payload Records ───────────────────────────────────────────────────────

    public record SubscriptionCreatedPayload(
        UUID subscriptionId, String tenantId, String msisdn,
        UUID productId, String productName, BillingCycle billingCycle,
        BigDecimal priceAmount, String priceCurrency,
        Instant startedAt, Instant nextBillingAt
    ) {}

    public record SubscriptionActivatedPayload(Instant billedAt, Instant nextBillingAt) {}

    public record SubscriptionRenewedPayload(Instant billedAt, Instant nextBillingAt) {}

    public record SubscriptionCancelledPayload(Instant cancelledAt, String reason) {}

    public record SubscriptionExpiredPayload(Instant expiredAt) {}

    public record SubscriptionSuspendedPayload(String reason) {}
}

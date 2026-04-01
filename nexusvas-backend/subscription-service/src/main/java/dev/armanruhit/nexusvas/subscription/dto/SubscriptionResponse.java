package dev.armanruhit.nexusvas.subscription.dto;

import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID subscriptionId,
    String tenantId,
    String msisdn,
    UUID productId,
    String productName,
    String status,
    String billingCycle,
    BigDecimal priceAmount,
    String priceCurrency,
    Instant startedAt,
    Instant nextBillingAt,
    Instant cancelledAt,
    int renewalCount
) {
    public static SubscriptionResponse from(SubscriptionProjection p) {
        return new SubscriptionResponse(
            p.getSubscriptionId(),
            p.getTenantId(),
            p.getMsisdn(),
            p.getProductId(),
            p.getProductName(),
            p.getStatus().name(),
            p.getBillingCycle().name(),
            p.getPriceAmount(),
            p.getPriceCurrency(),
            p.getStartedAt(),
            p.getNextBillingAt(),
            p.getCancelledAt(),
            p.getRenewalCount()
        );
    }
}

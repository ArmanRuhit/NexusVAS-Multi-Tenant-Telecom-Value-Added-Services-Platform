package dev.armanruhit.nexusvas.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeRequest(
    String tenantId,
    String msisdn,
    UUID subscriptionId,
    String productName,
    BigDecimal amount,
    String currency,
    String billingCycleDate  // used for idempotency key
) {}

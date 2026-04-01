package dev.armanruhit.nexusvas.subscription.dto;

import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSubscriptionCommand(
    @NotBlank String tenantId,
    @NotBlank String msisdn,
    @NotNull UUID productId,
    @NotBlank String productName,
    @NotNull BillingCycle billingCycle,
    @NotNull @Positive BigDecimal priceAmount,
    @NotBlank String priceCurrency
) {}

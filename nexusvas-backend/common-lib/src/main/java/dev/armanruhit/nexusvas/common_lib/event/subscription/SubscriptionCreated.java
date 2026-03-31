package dev.armanruhit.nexusvas.common_lib.event.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.armanruhit.nexusvas.common_lib.enums.BillingCycleEnum;
import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCreated(
    UUID subscriptionId,
    String msisdn,
    UUID productId,
    String productName,
    BillingCycleEnum billingCycle,
    Instant expiresAt
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper, String tenantId) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(
            EventType.SUBSCRIPTION_CREATED,
            tenantId,
            subscriptionId.toString(),
            1,
            payload
        );
    }
}
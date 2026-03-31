package dev.armanruhit.nexusvas.common_lib.event.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeSucceeded(
    UUID chargeId,
    String msisdn,
    BigDecimal amount,
    String currency,
    UUID subscriptionId
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper, String tenantId) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(
            EventType.CHARGE_SUCCEEDED,
            tenantId,
            chargeId.toString(),
            1,
            payload
        );
    }
}
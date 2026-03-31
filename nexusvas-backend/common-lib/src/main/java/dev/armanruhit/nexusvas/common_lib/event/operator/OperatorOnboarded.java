package dev.armanruhit.nexusvas.common_lib.event.operator;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

public record OperatorOnboarded(
    UUID operatorId,
    String name,
    String country,
    String timezone,
    String currency
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(EventType.OPERATOR_ONBOARDED, operatorId.toString(), operatorId.toString(), 1, payload);
    }
} 

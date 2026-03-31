package dev.armanruhit.nexusvas.common_lib.event.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

import java.util.UUID;

public record OperatorSuspended(
    UUID operatorId,
    String reason
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper, int version) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(
            EventType.OPERATOR_SUSPENDED,
            operatorId.toString(),
            operatorId.toString(),
            version,
            payload
        );
    }
}
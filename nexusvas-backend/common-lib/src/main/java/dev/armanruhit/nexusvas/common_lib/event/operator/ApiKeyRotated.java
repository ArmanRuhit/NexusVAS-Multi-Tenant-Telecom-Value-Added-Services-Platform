package dev.armanruhit.nexusvas.common_lib.event.operator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

import java.util.UUID;

public record ApiKeyRotated(
    UUID operatorId,
    UUID oldKeyId,
    UUID newKeyId
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper, int version) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(
            EventType.API_KEY_ROTATED,
            operatorId.toString(),
            operatorId.toString(),
            version,
            payload
        );
    }
}
package dev.armanruhit.nexusvas.common_lib.event.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

import java.util.UUID;

public record TokenRevoked(
    String jti,           // JWT ID
    UUID userId,
    String reason
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper, String tenantId) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(
            EventType.TOKEN_REVOKED,
            tenantId,
            userId.toString(),
            1,
            payload
        );
    }
}
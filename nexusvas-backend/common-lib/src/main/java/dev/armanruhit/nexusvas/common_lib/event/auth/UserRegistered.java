package dev.armanruhit.nexusvas.common_lib.event.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.common_lib.event.DomainEvent;
import dev.armanruhit.nexusvas.common_lib.event.EventType;

import java.util.UUID;

public record UserRegistered(
    UUID userId,
    String email,
    String userType,  // ADMIN, OPERATOR_USER, SUBSCRIBER
    String tenantId,
    String msisdn     // for subscribers
) {
    public DomainEvent toDomainEvent(ObjectMapper mapper) {
        JsonNode payload = mapper.valueToTree(this);
        return DomainEvent.create(
            EventType.USER_REGISTERED,
            tenantId,
            userId.toString(),
            1,
            payload
        );
    }
}
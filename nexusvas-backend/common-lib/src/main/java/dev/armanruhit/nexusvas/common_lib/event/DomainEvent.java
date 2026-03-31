package dev.armanruhit.nexusvas.common_lib.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record DomainEvent(
    UUID eventId,
    String eventType,
    String tenantId,
    String aggregateId,
    Instant timestamp,
    int version,
    JsonNode payload
) {
    public static DomainEvent create(String eventType, String tenantId, String aggregateId, int version, JsonNode payload) {
        return new DomainEvent(UUID.randomUUID(), eventType, tenantId, aggregateId, Instant.now(), version, payload);
    }
}

package dev.armanruhit.nexusvas.subscription.domain.aggregate;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionEvent;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.BillingCycle;
import dev.armanruhit.nexusvas.subscription.domain.aggregate.SubscriptionState.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subscription aggregate root — rebuilt by replaying events from the event store.
 * All state mutations go through apply() to maintain a consistent event log.
 */
@Slf4j
public class SubscriptionAggregate {

    public static final String AGGREGATE_TYPE = "Subscription";

    private final String aggregateId;
    private final SubscriptionState state;
    private final List<SubscriptionEvent> pendingEvents = new ArrayList<>();
    private final ObjectMapper objectMapper;

    public SubscriptionAggregate(String aggregateId, ObjectMapper objectMapper) {
        this.aggregateId = aggregateId;
        this.objectMapper = objectMapper;
        this.state = new SubscriptionState();
    }

    // ── Reconstitution from event store ──────────────────────────────────────

    public static SubscriptionAggregate reconstitute(
            String aggregateId, List<SubscriptionEvent> events, ObjectMapper objectMapper) {
        SubscriptionAggregate aggregate = new SubscriptionAggregate(aggregateId, objectMapper);
        for (SubscriptionEvent event : events) {
            aggregate.replayEvent(event);
        }
        return aggregate;
    }

    private void replayEvent(SubscriptionEvent event) {
        try {
            switch (event.getEventType()) {
                case "SubscriptionCreated" -> state.apply(
                    objectMapper.readValue(event.getPayload(), SubscriptionCreatedPayload.class));
                case "SubscriptionActivated" -> state.apply(
                    objectMapper.readValue(event.getPayload(), SubscriptionActivatedPayload.class));
                case "SubscriptionRenewed" -> state.apply(
                    objectMapper.readValue(event.getPayload(), SubscriptionRenewedPayload.class));
                case "SubscriptionCancelled" -> state.apply(
                    objectMapper.readValue(event.getPayload(), SubscriptionCancelledPayload.class));
                case "SubscriptionExpired" -> state.apply(
                    objectMapper.readValue(event.getPayload(), SubscriptionExpiredPayload.class));
                case "SubscriptionSuspended" -> state.apply(
                    objectMapper.readValue(event.getPayload(), SubscriptionSuspendedPayload.class));
                default -> log.warn("Unknown event type during replay: {}", event.getEventType());
            }
            state.incrementVersion();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to replay event: " + event.getEventType(), e);
        }
    }

    // ── Command Handlers ──────────────────────────────────────────────────────

    public void create(String tenantId, String msisdn, UUID productId, String productName,
                       BillingCycle billingCycle, BigDecimal priceAmount, String priceCurrency) {
        if (!state.isNew()) {
            throw new IllegalStateException("Subscription already exists: " + aggregateId);
        }

        Instant now = Instant.now();
        Instant nextBilling = computeNextBilling(now, billingCycle);

        SubscriptionCreatedPayload payload = new SubscriptionCreatedPayload(
            UUID.fromString(aggregateId), tenantId, msisdn, productId, productName,
            billingCycle, priceAmount, priceCurrency, now, nextBilling
        );

        appendEvent("SubscriptionCreated", tenantId, payload);
    }

    public void activate(Instant billedAt) {
        if (state.isNew()) {
            throw new IllegalStateException("Cannot activate a non-existent subscription");
        }

        Instant nextBilling = computeNextBilling(billedAt, state.getBillingCycle());
        appendEvent("SubscriptionActivated", state.getTenantId(),
            new SubscriptionActivatedPayload(billedAt, nextBilling));
    }

    public void renew() {
        if (!state.isActive()) {
            throw new IllegalStateException("Cannot renew subscription with status: " + state.getStatus());
        }

        Instant now = Instant.now();
        Instant nextBilling = computeNextBilling(now, state.getBillingCycle());
        appendEvent("SubscriptionRenewed", state.getTenantId(),
            new SubscriptionRenewedPayload(now, nextBilling));
    }

    public void cancel(String reason) {
        if (state.isCancelled()) {
            throw new IllegalStateException("Subscription is already cancelled");
        }

        appendEvent("SubscriptionCancelled", state.getTenantId(),
            new SubscriptionCancelledPayload(Instant.now(), reason));
    }

    public void expire() {
        appendEvent("SubscriptionExpired", state.getTenantId(),
            new SubscriptionExpiredPayload(Instant.now()));
    }

    public void suspend(String reason) {
        appendEvent("SubscriptionSuspended", state.getTenantId(),
            new SubscriptionSuspendedPayload(reason));
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getAggregateId() { return aggregateId; }
    public SubscriptionState getState() { return state; }
    public List<SubscriptionEvent> getPendingEvents() { return List.copyOf(pendingEvents); }
    public void clearPendingEvents() { pendingEvents.clear(); }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void appendEvent(String eventType, String tenantId, Object payload) {
        try {
            int nextVersion = state.getVersion() + 1;
            String payloadJson = objectMapper.writeValueAsString(payload);

            SubscriptionEvent event = SubscriptionEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .tenantId(tenantId)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .version(nextVersion)
                .payload(payloadJson)
                .build();

            pendingEvents.add(event);
            replayEvent(event); // apply optimistically
        } catch (Exception e) {
            throw new IllegalStateException("Failed to append event: " + eventType, e);
        }
    }

    private Instant computeNextBilling(Instant from, BillingCycle cycle) {
        return switch (cycle) {
            case DAILY   -> from.plusSeconds(86_400);
            case WEEKLY  -> from.plusSeconds(86_400 * 7);
            case MONTHLY -> from.plusSeconds(86_400L * 30);
        };
    }
}

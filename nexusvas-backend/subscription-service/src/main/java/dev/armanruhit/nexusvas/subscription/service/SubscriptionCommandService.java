package dev.armanruhit.nexusvas.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.subscription.domain.aggregate.SubscriptionAggregate;
import dev.armanruhit.nexusvas.subscription.domain.entity.OutboxEvent;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionEvent;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.BillingCycle;
import dev.armanruhit.nexusvas.subscription.domain.repository.OutboxEventRepository;
import dev.armanruhit.nexusvas.subscription.domain.repository.SubscriptionEventRepository;
import dev.armanruhit.nexusvas.subscription.domain.repository.SubscriptionProjectionRepository;
import dev.armanruhit.nexusvas.subscription.dto.CreateSubscriptionCommand;
import dev.armanruhit.nexusvas.subscription.exception.SubscriptionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionCommandService {

    private final SubscriptionEventRepository eventRepository;
    private final OutboxEventRepository outboxRepository;
    private final SubscriptionProjectionRepository projectionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SUB_CACHE_PREFIX = "sub:";
    private static final int CACHE_TTL_SECONDS = 300;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public UUID createSubscription(CreateSubscriptionCommand cmd) {
        // Idempotency: prevent duplicate active subscriptions
        projectionRepository.findActiveSubscription(
            cmd.tenantId(), cmd.msisdn(), cmd.productId()
        ).ifPresent(existing -> {
            throw new SubscriptionException("DUPLICATE_SUBSCRIPTION",
                "Active subscription already exists for this product");
        });

        UUID subscriptionId = UUID.randomUUID();

        SubscriptionAggregate aggregate = new SubscriptionAggregate(
            subscriptionId.toString(), objectMapper);
        aggregate.create(
            cmd.tenantId(), cmd.msisdn(), cmd.productId(), cmd.productName(),
            cmd.billingCycle(), cmd.priceAmount(), cmd.priceCurrency()
        );

        persistAndPublish(aggregate);

        log.info("Created subscription {} for msisdn {} on tenant {}",
            subscriptionId, cmd.msisdn(), cmd.tenantId());

        return subscriptionId;
    }

    // ── Activate (called by Billing Service via Kafka on ChargeSucceeded) ─────

    @Transactional
    public void activateSubscription(UUID subscriptionId, String tenantId) {
        SubscriptionAggregate aggregate = load(subscriptionId.toString(), tenantId);
        aggregate.activate(java.time.Instant.now());
        persistAndPublish(aggregate);
        invalidateCache(tenantId, aggregate.getState().getMsisdn(), subscriptionId);

        log.info("Activated subscription {}", subscriptionId);
    }

    // ── Renew ─────────────────────────────────────────────────────────────────

    @Transactional
    public void renewSubscription(UUID subscriptionId, String tenantId) {
        SubscriptionAggregate aggregate = load(subscriptionId.toString(), tenantId);
        aggregate.renew();
        persistAndPublish(aggregate);
        invalidateCache(tenantId, aggregate.getState().getMsisdn(), subscriptionId);

        log.info("Renewed subscription {}", subscriptionId);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelSubscription(UUID subscriptionId, String tenantId, String reason) {
        SubscriptionAggregate aggregate = load(subscriptionId.toString(), tenantId);
        aggregate.cancel(reason);
        persistAndPublish(aggregate);
        invalidateCache(tenantId, aggregate.getState().getMsisdn(), subscriptionId);

        log.info("Cancelled subscription {} reason={}", subscriptionId, reason);
    }

    // ── Expire ────────────────────────────────────────────────────────────────

    @Transactional
    public void expireSubscription(UUID subscriptionId, String tenantId) {
        SubscriptionAggregate aggregate = load(subscriptionId.toString(), tenantId);
        aggregate.expire();
        persistAndPublish(aggregate);
        invalidateCache(tenantId, aggregate.getState().getMsisdn(), subscriptionId);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private SubscriptionAggregate load(String aggregateId, String tenantId) {
        List<SubscriptionEvent> events = eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId);
        if (events.isEmpty()) {
            throw new SubscriptionException("NOT_FOUND", "Subscription not found: " + aggregateId);
        }
        return SubscriptionAggregate.reconstitute(aggregateId, events, objectMapper);
    }

    private void persistAndPublish(SubscriptionAggregate aggregate) {
        List<SubscriptionEvent> newEvents = aggregate.getPendingEvents();

        // Persist events to the event store
        eventRepository.saveAll(newEvents);

        // Write outbox entries in the same transaction (at-least-once delivery guarantee)
        for (SubscriptionEvent event : newEvents) {
            OutboxEvent outbox = OutboxEvent.builder()
                .aggregateType(SubscriptionAggregate.AGGREGATE_TYPE)
                .aggregateId(aggregate.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();
            outboxRepository.save(outbox);
        }

        aggregate.clearPendingEvents();
    }

    private void invalidateCache(String tenantId, String msisdn, UUID subscriptionId) {
        redisTemplate.delete(SUB_CACHE_PREFIX + tenantId + ":" + msisdn);
        redisTemplate.delete(SUB_CACHE_PREFIX + subscriptionId);
    }
}

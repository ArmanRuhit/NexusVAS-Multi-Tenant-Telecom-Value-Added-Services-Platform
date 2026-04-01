package dev.armanruhit.nexusvas.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.armanruhit.nexusvas.subscription.domain.aggregate.SubscriptionState.*;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection;
import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.SubscriptionStatus;
import dev.armanruhit.nexusvas.subscription.domain.repository.SubscriptionProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Consumes subscription events from Kafka and updates the read-side projection.
 * This is the CQRS query side — it keeps subscription_projections in sync.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionProjectionService {

    private final SubscriptionProjectionRepository projectionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SUB_CACHE_PREFIX = "sub:";
    private static final int CACHE_TTL_SECONDS = 300;

    // ── Kafka Listener (CQRS read-side updater) ───────────────────────────────

    @KafkaListener(topics = "subscription-events", groupId = "subscription-projection-group")
    @Transactional
    public void onSubscriptionEvent(@Payload String payloadJson,
                                    @Header("eventType") String eventType,
                                    @Header("aggregateId") String aggregateId) {
        try {
            switch (eventType) {
                case "SubscriptionCreated" -> handleCreated(aggregateId,
                    objectMapper.readValue(payloadJson, SubscriptionCreatedPayload.class));
                case "SubscriptionActivated" -> handleActivated(aggregateId,
                    objectMapper.readValue(payloadJson, SubscriptionActivatedPayload.class));
                case "SubscriptionRenewed" -> handleRenewed(aggregateId,
                    objectMapper.readValue(payloadJson, SubscriptionRenewedPayload.class));
                case "SubscriptionCancelled" -> handleCancelled(aggregateId,
                    objectMapper.readValue(payloadJson, SubscriptionCancelledPayload.class));
                case "SubscriptionExpired" -> handleExpired(aggregateId,
                    objectMapper.readValue(payloadJson, SubscriptionExpiredPayload.class));
                case "SubscriptionSuspended" -> handleSuspended(aggregateId,
                    objectMapper.readValue(payloadJson, SubscriptionSuspendedPayload.class));
                default -> log.debug("Ignoring unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process subscription event {} for aggregate {}: {}",
                eventType, aggregateId, e.getMessage(), e);
            throw new RuntimeException("Projection update failed", e); // trigger Kafka retry
        }
    }

    private void handleCreated(String aggregateId, SubscriptionCreatedPayload p) {
        SubscriptionProjection projection = SubscriptionProjection.builder()
            .subscriptionId(p.subscriptionId())
            .tenantId(p.tenantId())
            .msisdn(p.msisdn())
            .productId(p.productId())
            .productName(p.productName())
            .billingCycle(p.billingCycle())
            .priceAmount(p.priceAmount())
            .priceCurrency(p.priceCurrency())
            .startedAt(p.startedAt())
            .nextBillingAt(p.nextBillingAt())
            .status(SubscriptionStatus.PENDING)
            .createdAt(p.startedAt())
            .version(1)
            .build();
        projectionRepository.save(projection);
    }

    private void handleActivated(String aggregateId, SubscriptionActivatedPayload p) {
        findAndUpdate(UUID.fromString(aggregateId), projection -> {
            projection.setStatus(SubscriptionStatus.ACTIVE);
            projection.setLastBilledAt(p.billedAt());
            projection.setNextBillingAt(p.nextBillingAt());
            projection.setVersion(projection.getVersion() + 1);
        });
    }

    private void handleRenewed(String aggregateId, SubscriptionRenewedPayload p) {
        findAndUpdate(UUID.fromString(aggregateId), projection -> {
            projection.setLastBilledAt(p.billedAt());
            projection.setNextBillingAt(p.nextBillingAt());
            projection.setRenewalCount(projection.getRenewalCount() + 1);
            projection.setStatus(SubscriptionStatus.ACTIVE);
            projection.setVersion(projection.getVersion() + 1);
        });
    }

    private void handleCancelled(String aggregateId, SubscriptionCancelledPayload p) {
        findAndUpdate(UUID.fromString(aggregateId), projection -> {
            projection.setStatus(SubscriptionStatus.CANCELLED);
            projection.setCancelledAt(p.cancelledAt());
            projection.setCancellationReason(p.reason());
            projection.setVersion(projection.getVersion() + 1);
        });
        invalidateCache(UUID.fromString(aggregateId));
    }

    private void handleExpired(String aggregateId, SubscriptionExpiredPayload p) {
        findAndUpdate(UUID.fromString(aggregateId), projection -> {
            projection.setStatus(SubscriptionStatus.EXPIRED);
            projection.setExpiresAt(p.expiredAt());
            projection.setVersion(projection.getVersion() + 1);
        });
        invalidateCache(UUID.fromString(aggregateId));
    }

    private void handleSuspended(String aggregateId, SubscriptionSuspendedPayload p) {
        findAndUpdate(UUID.fromString(aggregateId), projection -> {
            projection.setStatus(SubscriptionStatus.SUSPENDED);
            projection.setVersion(projection.getVersion() + 1);
        });
        invalidateCache(UUID.fromString(aggregateId));
    }

    private void findAndUpdate(UUID subscriptionId, java.util.function.Consumer<SubscriptionProjection> updater) {
        projectionRepository.findBySubscriptionId(subscriptionId).ifPresentOrElse(
            projection -> {
                updater.accept(projection);
                projectionRepository.save(projection);
            },
            () -> log.warn("Projection not found for subscriptionId: {}", subscriptionId)
        );
    }

    // ── Query Methods ─────────────────────────────────────────────────────────

    public Page<SubscriptionProjection> list(String tenantId, SubscriptionStatus status, Pageable pageable) {
        if (status != null) {
            return projectionRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        }
        return projectionRepository.findByTenantId(tenantId, pageable);
    }

    public Optional<SubscriptionProjection> findById(UUID subscriptionId, String tenantId) {
        return projectionRepository.findBySubscriptionId(subscriptionId)
            .filter(p -> p.getTenantId().equals(tenantId));
    }

    public boolean isActiveSubscription(String tenantId, String msisdn, UUID productId) {
        String cacheKey = SUB_CACHE_PREFIX + tenantId + ":" + msisdn + ":" + productId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return "true".equals(cached);
        }

        boolean active = projectionRepository.findActiveSubscription(tenantId, msisdn, productId).isPresent();
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(active), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return active;
    }

    private void invalidateCache(UUID subscriptionId) {
        projectionRepository.findBySubscriptionId(subscriptionId).ifPresent(p ->
            redisTemplate.delete(SUB_CACHE_PREFIX + p.getTenantId() + ":" + p.getMsisdn() + ":" + p.getProductId())
        );
    }
}

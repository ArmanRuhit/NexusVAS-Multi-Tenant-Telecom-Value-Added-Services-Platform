package dev.armanruhit.nexusvas.content.service;

import dev.armanruhit.nexusvas.content.domain.document.ContentItem;
import dev.armanruhit.nexusvas.content.domain.repository.ContentItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Reactive content delivery — validates active subscription via Redis cache
 * (or falls back to Subscription Service), then streams published content.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentDeliveryService {

    private final ContentItemRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;

    private static final String SUB_CACHE_PREFIX = "sub:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public Flux<ContentItem> deliver(String tenantId, String msisdn, String productId, int page, int size) {
        return isSubscriptionActive(tenantId, msisdn, productId)
            .flatMapMany(active -> {
                if (!active) {
                    return Flux.error(new SubscriptionInactiveException(
                        "No active subscription for product: " + productId));
                }
                return repository.findByTenantIdAndTargetProductsContainingAndStatus(
                    tenantId, productId, ContentItem.ContentStatus.PUBLISHED,
                    PageRequest.of(page, size));
            });
    }

    private Mono<Boolean> isSubscriptionActive(String tenantId, String msisdn, String productId) {
        String cacheKey = SUB_CACHE_PREFIX + tenantId + ":" + msisdn + ":" + productId;

        return redisTemplate.opsForValue().get(cacheKey)
            .map("true"::equals)
            .switchIfEmpty(
                // Cache miss — call Subscription Service
                webClientBuilder.build()
                    .get()
                    .uri("http://subscription-service/api/v1/subscriptions/check",
                        builder -> builder
                            .queryParam("msisdn", msisdn)
                            .queryParam("productId", productId)
                            .build())
                    .retrieve()
                    .bodyToMono(SubscriptionCheckResponse.class)
                    .map(SubscriptionCheckResponse::active)
                    .flatMap(active ->
                        redisTemplate.opsForValue()
                            .set(cacheKey, String.valueOf(active), CACHE_TTL)
                            .thenReturn(active))
                    .onErrorReturn(false) // fail open to not block delivery on service errors
            );
    }

    public static class SubscriptionInactiveException extends RuntimeException {
        public SubscriptionInactiveException(String message) { super(message); }
    }

    private record SubscriptionCheckResponse(boolean active) {}
}

package dev.armanruhit.nexusvas.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Sliding-window rate limiter using Redis.
 * Key: rl:{tenantId}:{windowMinute}
 * Increments counter per minute window; rejects when limit exceeded.
 */
@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${gateway.rate-limit.requests-per-minute:300}")
    private int requestsPerMinute;

    public RateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        if (tenantId == null) {
            // No tenant header yet — JWT filter hasn't run or anonymous request; skip
            return chain.filter(exchange);
        }

        long windowMinute = System.currentTimeMillis() / 60_000;
        String key = "rl:" + tenantId + ":" + windowMinute;

        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    // First request in this window — set TTL
                    return redisTemplate.expire(key, Duration.ofMinutes(2))
                        .thenReturn(count);
                }
                return Mono.just(count);
            })
            .flatMap(count -> {
                if (count > requestsPerMinute) {
                    log.warn("Rate limit exceeded for tenant {} count={}", tenantId, count);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit",
                        String.valueOf(requestsPerMinute));
                    exchange.getResponse().getHeaders().add("Retry-After", "60");
                    return exchange.getResponse().setComplete();
                }
                return chain.filter(exchange);
            })
            .onErrorResume(e -> {
                // Redis unavailable — fail open to not block traffic
                log.error("Rate limit Redis error for tenant {}: {}", tenantId, e.getMessage());
                return chain.filter(exchange);
            });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 50;
    }
}

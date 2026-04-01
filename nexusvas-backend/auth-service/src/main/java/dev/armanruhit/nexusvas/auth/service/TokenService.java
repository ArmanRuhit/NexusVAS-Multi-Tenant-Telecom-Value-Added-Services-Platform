package dev.armanruhit.nexusvas.auth.service;

import dev.armanruhit.nexusvas.auth.domain.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KeyPair jwtKeyPair;

    @Value("${auth.jwt.access-token-ttl-minutes:15}")
    private int accessTokenTtlMinutes;

    @Value("${auth.jwt.refresh-token-ttl-days:7}")
    private int refreshTokenTtlDays;

    @Value("${auth.jwt.issuer:http://localhost:8081}")
    private String issuer;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public String generateAccessToken(User user, String clientType) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenTtlMinutes * 60L);

        Set<String> permissions = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(p -> p.getName())
            .collect(Collectors.toSet());

        Set<String> roles = user.getRoles().stream()
            .map(r -> r.getName())
            .collect(Collectors.toSet());

        return Jwts.builder()
            .id(jti)
            .subject(user.getId().toString())
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("tenant_id", user.getTenantId())
            .claim("email", user.getEmail())
            .claim("roles", roles)
            .claim("permissions", permissions)
            .claim("client_type", clientType)
            .signWith(jwtKeyPair.getPrivate())
            .compact();
    }

    public String generateSubscriberAccessToken(String msisdn, String tenantId) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600L); // 1 hour for subscribers

        return Jwts.builder()
            .id(jti)
            .subject(msisdn)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("tenant_id", tenantId)
            .claim("roles", List.of("SUBSCRIBER"))
            .claim("client_type", "SUBSCRIBER")
            .signWith(jwtKeyPair.getPrivate())
            .compact();
    }

    public String generateRefreshToken(UUID userId, String tenantId, String deviceId) {
        String token = UUID.randomUUID().toString();
        String tokenHash = hashToken(token);

        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("tenantId", tenantId != null ? tenantId : "");
        payload.put("deviceId", deviceId != null ? deviceId : "");

        redisTemplate.opsForHash().putAll(REFRESH_TOKEN_PREFIX + tokenHash, payload);
        redisTemplate.expire(REFRESH_TOKEN_PREFIX + tokenHash, refreshTokenTtlDays, TimeUnit.DAYS);

        return token;
    }

    public Optional<Map<Object, Object>> validateRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        String key = REFRESH_TOKEN_PREFIX + tokenHash;

        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(data);
    }

    public void revokeRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + tokenHash);
    }

    public void blacklistAccessToken(String jti, Instant expiry) {
        long ttlSeconds = Duration.between(Instant.now(), expiry).getSeconds();
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "revoked", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith((java.security.interfaces.RSAPublicKey) jwtKeyPair.getPublic())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public void revokeAllUserRefreshTokens(UUID userId) {
        // Scan for all refresh tokens belonging to this user
        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_PREFIX + "*");
        if (keys == null) return;

        for (String key : keys) {
            Object storedUserId = redisTemplate.opsForHash().get(key, "userId");
            if (userId.toString().equals(storedUserId)) {
                redisTemplate.delete(key);
            }
        }
    }

    private String hashToken(String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }
}

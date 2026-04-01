package dev.armanruhit.nexusvas.auth.service;

import dev.armanruhit.nexusvas.auth.domain.entity.ApiKey;
import dev.armanruhit.nexusvas.auth.domain.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    private static final String KEY_PREFIX = "nvk_";

    @Transactional
    public String createApiKey(String tenantId, String name, String scopes,
                               String rateLimitTier, Instant expiresAt, UUID createdBy) {
        String rawKey = KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String keyHash = hashKey(rawKey);
        String keyPrefix = rawKey.substring(0, Math.min(8, rawKey.length()));

        ApiKey apiKey = ApiKey.builder()
            .tenantId(tenantId)
            .keyHash(keyHash)
            .keyPrefix(keyPrefix)
            .name(name)
            .scopes(scopes)
            .rateLimitTier(rateLimitTier != null ? rateLimitTier : "STANDARD")
            .expiresAt(expiresAt)
            .status(ApiKey.ApiKeyStatus.ACTIVE)
            .createdBy(createdBy)
            .build();

        apiKeyRepository.save(apiKey);
        log.info("Created API key '{}' for tenant {}", name, tenantId);

        // Return the raw key only once — it's never stored in plaintext
        return rawKey;
    }

    @Transactional(readOnly = true)
    public Optional<ApiKey> validateApiKey(String rawKey) {
        String keyHash = hashKey(rawKey);
        return apiKeyRepository.findByKeyHash(keyHash)
            .filter(ApiKey::isValid);
    }

    @Transactional
    public void revokeApiKey(UUID keyId, UUID revokedBy) {
        apiKeyRepository.findById(keyId).ifPresent(key -> {
            key.setStatus(ApiKey.ApiKeyStatus.REVOKED);
            key.setRevokedAt(Instant.now());
            key.setRevokedBy(revokedBy);
            apiKeyRepository.save(key);
            log.info("Revoked API key: {}", keyId);
        });
    }

    @Transactional
    public void updateLastUsed(UUID keyId) {
        apiKeyRepository.findById(keyId).ifPresent(key -> {
            key.setLastUsedAt(Instant.now());
            apiKeyRepository.save(key);
        });
    }

    public List<ApiKey> listTenantKeys(String tenantId) {
        return apiKeyRepository.findByTenantIdAndStatusActive(tenantId);
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash API key", e);
        }
    }
}

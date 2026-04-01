package dev.armanruhit.nexusvas.content.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates signed CDN URLs for secure content delivery.
 * Supports CloudFront-style signed URLs with expiration.
 */
@Service
@Slf4j
public class CdnUrlService {

    @Value("${cdn.enabled:false}")
    private boolean cdnEnabled;

    @Value("${cdn.domain:cdn.nexusvas.com}")
    private String cdnDomain;

    @Value("${cdn.private-key:}")
    private String cdnPrivateKey;

    @Value("${cdn.key-pair-id:}")
    private String keyPairId;

    @Value("${cdn.url-expiration-hours:1}")
    private int urlExpirationHours;

    // Per-tenant CDN domain mapping (operators can have custom CDN domains)
    private final Map<String, String> tenantCdnDomains = new ConcurrentHashMap<>();

    /**
     * Generates a signed CDN URL for the given content path.
     * Falls back to original URL if CDN is not configured.
     */
    public Mono<String> generateSignedUrl(String originalUrl, String tenantId) {
        if (!cdnEnabled || originalUrl == null || originalUrl.isBlank()) {
            return Mono.justOrEmpty(originalUrl);
        }

        // Extract path from original URL
        String path = extractPath(originalUrl);
        if (path == null) {
            return Mono.just(originalUrl);
        }

        // Get tenant-specific CDN domain or use default
        String domain = tenantCdnDomains.getOrDefault(tenantId, cdnDomain);

        // Generate signed URL
        return Mono.fromCallable(() -> {
            String signedUrl = generateCloudFrontSignedUrl(domain, path, tenantId);
            log.debug("Generated signed CDN URL for path={} tenant={}", path, tenantId);
            return signedUrl;
        });
    }

    /**
     * Generates a manifest of CDN URLs for offline content packs.
     */
    public Mono<Map<String, String>> generateManifestUrls(Map<String, String> originalUrls, String tenantId) {
        if (!cdnEnabled) {
            return Mono.just(originalUrls);
        }

        return Mono.fromCallable(() -> {
            Map<String, String> signedUrls = new ConcurrentHashMap<>();
            originalUrls.forEach((key, url) -> {
                String signed = generateCloudFrontSignedUrl(
                    tenantCdnDomains.getOrDefault(tenantId, cdnDomain),
                    extractPath(url),
                    tenantId
                );
                signedUrls.put(key, signed);
            });
            return signedUrls;
        });
    }

    /**
     * Registers a custom CDN domain for a tenant.
     */
    public void registerTenantCdnDomain(String tenantId, String domain) {
        tenantCdnDomains.put(tenantId, domain);
        log.info("Registered CDN domain {} for tenant {}", domain, tenantId);
    }

    private String extractPath(String url) {
        if (url == null) return null;
        try {
            int schemeEnd = url.indexOf("://");
            String afterScheme = schemeEnd >= 0 ? url.substring(schemeEnd + 3) : url;
            int pathStart = afterScheme.indexOf('/');
            return pathStart >= 0 ? afterScheme.substring(pathStart) : "/" + afterScheme;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates a CloudFront-style signed URL using RSA-SHA1.
     * For production, use actual CloudFront SDK or your CDN provider's SDK.
     */
    private String generateCloudFrontSignedUrl(String domain, String path, String tenantId) {
        if (cdnPrivateKey == null || cdnPrivateKey.isBlank()) {
            // No signing key configured - return unsigned URL
            return String.format("https://%s%s", domain, path);
        }

        try {
            // Calculate expiration time
            Instant expiration = Instant.now().plus(urlExpirationHours, ChronoUnit.HOURS);
            long expiresEpoch = expiration.getEpochSecond();

            // Build policy statement
            String resource = String.format("https://%s%s", domain, path);
            String policy = String.format(
                "{\"Statement\":[{\"Resource\":\"%s\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":%d}}}]}",
                resource, expiresEpoch
            );

            // Sign the policy (simplified - in production use proper RSA signing)
            String signature = signPolicy(policy);

            // Build signed URL
            return String.format(
                "https://%s%s?Expires=%d&Signature=%s&Key-Pair-Id=%s",
                domain, path, expiresEpoch,
                URLEncoder.encode(signature, StandardCharsets.UTF_8),
                keyPairId
            );
        } catch (Exception e) {
            log.warn("Failed to sign CDN URL, returning unsigned: {}", e.getMessage());
            return String.format("https://%s%s", domain, path);
        }
    }

    private String signPolicy(String policy) throws Exception {
        // Simplified signing - in production use proper RSA-SHA1 with your private key
        // This is a placeholder that creates a deterministic signature
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            cdnPrivateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] signature = mac.doFinal(policy.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature)
            .replace("+", "-")
            .replace("=", "_")
            .replace("/", "~");
    }
}

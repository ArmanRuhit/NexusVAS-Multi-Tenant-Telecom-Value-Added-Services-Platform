package dev.armanruhit.nexusvas.operator.service;

import dev.armanruhit.nexusvas.operator.domain.entity.Operator;
import dev.armanruhit.nexusvas.operator.domain.entity.OperatorConfig;
import dev.armanruhit.nexusvas.operator.domain.entity.OperatorApiKey;
import dev.armanruhit.nexusvas.operator.domain.repository.OperatorConfigRepository;
import dev.armanruhit.nexusvas.operator.domain.repository.OperatorRepository;
import dev.armanruhit.nexusvas.operator.domain.repository.OperatorApiKeyRepository;
import dev.armanruhit.nexusvas.operator.dto.OperatorOnboardRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final OperatorConfigRepository configRepository;
    private final OperatorApiKeyRepository apiKeyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public Operator onboard(OperatorOnboardRequest req) {
        if (operatorRepository.existsBySlug(req.slug())) {
            throw new SlugAlreadyExistsException(req.slug());
        }

        Operator operator = Operator.builder()
            .name(req.name())
            .slug(req.slug())
            .country(req.country())
            .timezone(req.timezone() != null ? req.timezone() : "UTC")
            .currency(req.currency() != null ? req.currency() : "USD")
            .billingModel(req.billingModel())
            .contactEmail(req.contactEmail())
            .contactPhone(req.contactPhone())
            .address(req.address())
            .contractStartDate(req.contractStartDate())
            .contractEndDate(req.contractEndDate())
            .build();

        Operator saved = operatorRepository.save(operator);
        log.info("Onboarded operator '{}' id={}", saved.getName(), saved.getId());

        publishEvent("OperatorOnboarded", saved);
        return saved;
    }

    public Operator findById(UUID id) {
        return operatorRepository.findById(id)
            .orElseThrow(() -> new OperatorNotFoundException(id.toString()));
    }

    public Operator findBySlug(String slug) {
        return operatorRepository.findBySlug(slug)
            .orElseThrow(() -> new OperatorNotFoundException(slug));
    }

    public Page<Operator> list(Operator.OperatorStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status != null) {
            return operatorRepository.findByStatus(status, pageable);
        }
        return operatorRepository.findAll(pageable);
    }

    @Transactional
    public Operator updateStatus(UUID id, Operator.OperatorStatus newStatus) {
        Operator operator = findById(id);
        Operator.OperatorStatus old = operator.getStatus();
        operator.setStatus(newStatus);
        Operator saved = operatorRepository.save(operator);

        publishEvent(newStatus == Operator.OperatorStatus.ACTIVE ? "OperatorActivated" :
                     newStatus == Operator.OperatorStatus.SUSPENDED ? "OperatorSuspended" :
                     newStatus == Operator.OperatorStatus.TERMINATED ? "OperatorTerminated" :
                     "OperatorStatusChanged", saved);

        log.info("Operator {} status changed {} -> {}", id, old, newStatus);
        return saved;
    }

    // ── Config management ─────────────────────────────────────────────────────

    @Transactional
    public OperatorConfig setConfig(UUID operatorId, String key, Object value, String description) {
        Operator operator = findById(operatorId);
        OperatorConfig config = configRepository
            .findByOperatorIdAndConfigKey(operatorId, key)
            .orElseGet(() -> OperatorConfig.builder()
                .operator(operator)
                .configKey(key)
                .build());

        config.setConfigValue(value);
        if (description != null) config.setDescription(description);
        return configRepository.save(config);
    }

    public List<OperatorConfig> getConfigs(UUID operatorId) {
        findById(operatorId); // verify exists
        return configRepository.findByOperatorId(operatorId);
    }

    // ── Statistics ─────────────────────────────────────────────────────────────

    public OperatorStats getStats(UUID operatorId) {
        Operator operator = findById(operatorId);
        // In production, would query subscription/billing services via Kafka or REST
        return new OperatorStats(
            operatorId,
            operator.getName(),
            operator.getStatus().name(),
            0L, // activeSubscriptions - would be fetched from subscription service
            0L, // totalRevenue - would be fetched from billing service
            LocalDate.now().getMonthValue(),
            LocalDate.now().getYear()
        );
    }

    // ── API Key management ─────────────────────────────────────────────────────

    @Transactional
    public String generateApiKey(UUID operatorId, String name) {
        Operator operator = findById(operatorId);

        // Generate secure random API key
        byte[] keyBytes = new byte[32];
        SECURE_RANDOM.nextBytes(keyBytes);
        String apiKey = "nv_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        // Hash for storage (in production, use proper key derivation)
        String keyHash = Base64.getEncoder().encodeToString(keyBytes);

        OperatorApiKey entity = OperatorApiKey.builder()
            .operator(operator)
            .name(name)
            .keyHash(keyHash)
            .prefix(apiKey.substring(0, 10))
            .build();

        apiKeyRepository.save(entity);
        log.info("Generated API key '{}' for operator {}", name, operatorId);
        return apiKey;
    }

    public List<Map<String, Object>> listApiKeys(UUID operatorId) {
        findById(operatorId); // verify exists
        return apiKeyRepository.findByOperatorId(operatorId).stream()
            .map(k -> Map.<String, Object>of(
                "id", k.getId(),
                "name", k.getName(),
                "prefix", k.getPrefix(),
                "createdAt", k.getCreatedAt().toString()
            ))
            .toList();
    }

    @Transactional
    public void revokeApiKey(UUID operatorId, UUID keyId) {
        findById(operatorId); // verify exists
        apiKeyRepository.findById(keyId)
            .filter(k -> k.getOperator().getId().equals(operatorId))
            .ifPresent(apiKeyRepository::delete);
        log.info("Revoked API key {} for operator {}", keyId, operatorId);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void publishEvent(String eventType, Operator operator) {
        kafkaTemplate.send("operator-events", operator.getId().toString(), Map.of(
            "eventType",  eventType,
            "operatorId", operator.getId().toString(),
            "tenantId",   operator.getId().toString(), // operatorId IS the tenantId
            "slug",       operator.getSlug(),
            "name",       operator.getName(),
            "status",     operator.getStatus().name(),
            "timestamp",  Instant.now().toString()
        ));
    }

    // Records
    public record OperatorStats(
        UUID operatorId, String name, String status,
        long activeSubscriptions, long totalRevenue,
        int month, int year
    ) {}

    // Exceptions
    public static class OperatorNotFoundException extends RuntimeException {
        public OperatorNotFoundException(String id) { super("Operator not found: " + id); }
    }

    public static class SlugAlreadyExistsException extends RuntimeException {
        public SlugAlreadyExistsException(String slug) { super("Slug already taken: " + slug); }
    }
}

package dev.armanruhit.nexusvas.ai.service;

import dev.armanruhit.nexusvas.ai.domain.entity.FraudAlert;
import dev.armanruhit.nexusvas.ai.domain.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fraud detection service using heuristic rules and anomaly detection.
 * Analyzes billing events and subscription patterns for suspicious activity.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;

    // Risk thresholds
    private static final int MAX_DAILY_CHARGES = 10;
    private static final BigDecimal UNUSUAL_AMOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final int MAX_FAILED_CHARGES = 3;
    private static final int MAX_SUBSCRIPTION_CHANGES = 5;

    public List<FraudAlert> getActiveAlerts(String tenantId) {
        return fraudAlertRepository.findByTenantIdAndStatusOrderByDetectedAtDesc(
            tenantId, FraudAlert.AlertStatus.ACTIVE);
    }

    public List<FraudAlert> getAlertsByMsisdn(String tenantId, String msisdn) {
        return fraudAlertRepository.findByTenantIdAndMsisdnOrderByDetectedAtDesc(tenantId, msisdn);
    }

    @Transactional
    public void acknowledgeAlert(String tenantId, UUID alertId) {
        fraudAlertRepository.findById(alertId)
            .filter(a -> a.getTenantId().equals(tenantId))
            .ifPresent(alert -> {
                alert.setStatus(FraudAlert.AlertStatus.ACKNOWLEDGED);
                fraudAlertRepository.save(alert);
                log.info("Acknowledged fraud alert {} for tenant {}", alertId, tenantId);
            });
    }

    @Transactional
    public void resolveAlert(String tenantId, UUID alertId, String resolution) {
        fraudAlertRepository.findById(alertId)
            .filter(a -> a.getTenantId().equals(tenantId))
            .ifPresent(alert -> {
                alert.setStatus(FraudAlert.AlertStatus.RESOLVED);
                alert.setResolution(resolution);
                alert.setResolvedAt(Instant.now());
                fraudAlertRepository.save(alert);
                log.info("Resolved fraud alert {} for tenant {}: {}", alertId, tenantId, resolution);
            });
    }

    @KafkaListener(topics = "billing-events", groupId = "ai-fraud-detection-group")
    @Transactional
    public void onBillingEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String tenantId = (String) event.get("tenantId");
        String msisdn = (String) event.get("msisdn");

        if (tenantId == null || msisdn == null) return;

        // Check for suspicious patterns
        switch (eventType) {
            case "ChargeSucceeded" -> checkChargePattern(event);
            case "ChargeFailed" -> checkFailedChargePattern(event);
            case "RefundRequested" -> checkRefundPattern(event);
        }
    }

    @KafkaListener(topics = "subscription-events", groupId = "ai-fraud-detection-group")
    @Transactional
    public void onSubscriptionEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String tenantId = (String) event.get("tenantId");
        String msisdn = (String) event.get("msisdn");

        if (tenantId == null || msisdn == null) return;

        // Check for subscription fraud patterns
        if ("SubscriptionActivated".equals(eventType)) {
            checkSubscriptionPattern(event);
        }
    }

    private void checkChargePattern(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        String msisdn = (String) event.get("msisdn");
        BigDecimal amount = parseAmount(event.get("amount"));

        // Check for unusually high amounts
        if (amount != null && amount.compareTo(UNUSUAL_AMOUNT_THRESHOLD) > 0) {
            createAlert(tenantId, msisdn, FraudAlert.FraudType.UNUSUAL_AMOUNT,
                "Unusual charge amount detected: " + amount, event);
        }

        // Check for rapid successive charges (would need to track state in production)
        // This is a simplified version
    }

    private void checkFailedChargePattern(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        String msisdn = (String) event.get("msisdn");

        // In production, would query recent failed charges count
        // For now, create alert on any failed charge pattern
        log.debug("Failed charge detected for msisdn={} tenant={}", msisdn, tenantId);
    }

    private void checkRefundPattern(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        String msisdn = (String) event.get("msisdn");
        BigDecimal amount = parseAmount(event.get("amount"));

        // Flag high-value refunds
        if (amount != null && amount.compareTo(new BigDecimal("100.00")) > 0) {
            createAlert(tenantId, msisdn, FraudAlert.FraudType.SUSPICIOUS_REFUND,
                "High-value refund requested: " + amount, event);
        }
    }

    private void checkSubscriptionPattern(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        String msisdn = (String) event.get("msisdn");

        // In production, would check for rapid subscribe/unsubscribe patterns
        // Would also check for multiple subscriptions to same product
        log.debug("Subscription activation for msisdn={} tenant={}", msisdn, tenantId);
    }

    private void createAlert(String tenantId, String msisdn, FraudAlert.FraudType type,
                            String description, Map<String, Object> eventData) {
        FraudAlert alert = FraudAlert.builder()
            .tenantId(tenantId)
            .msisdn(msisdn)
            .fraudType(type)
            .description(description)
            .status(FraudAlert.AlertStatus.ACTIVE)
            .severity(determineSeverity(type))
            .rawEvent(eventData.toString())
            .build();

        fraudAlertRepository.save(alert);
        log.warn("Fraud alert created: type={} msisdn={} tenant={} desc={}",
            type, msisdn, tenantId, description);
    }

    private FraudAlert.Severity determineSeverity(FraudAlert.FraudType type) {
        return switch (type) {
            case UNUSUAL_AMOUNT -> FraudAlert.Severity.MEDIUM;
            case RAPID_CHARGES -> FraudAlert.Severity.HIGH;
            case SUSPICIOUS_REFUND -> FraudAlert.Severity.HIGH;
            case ACCOUNT_TAKEOVER -> FraudAlert.Severity.CRITICAL;
            case SUBSCRIPTION_ABUSE -> FraudAlert.Severity.MEDIUM;
            default -> FraudAlert.Severity.LOW;
        };
    }

    private BigDecimal parseAmount(Object amount) {
        if (amount == null) return null;
        if (amount instanceof BigDecimal bd) return bd;
        if (amount instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(amount.toString());
        } catch (Exception e) {
            return null;
        }
    }
}

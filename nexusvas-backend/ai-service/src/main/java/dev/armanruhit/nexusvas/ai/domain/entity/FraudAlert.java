package dev.armanruhit.nexusvas.ai.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Fraud alert entity for tracking suspicious activities.
 */
@Entity
@Table(name = "fraud_alerts", indexes = {
    @Index(name = "idx_fraud_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_fraud_tenant_msisdn", columnList = "tenant_id, msisdn"),
    @Index(name = "idx_fraud_detected_at", columnList = "detected_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "msisdn", nullable = false)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_type", nullable = false)
    private FraudType fraudType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "raw_event", columnDefinition = "TEXT")
    private String rawEvent;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }

    public enum FraudType {
        UNUSUAL_AMOUNT,       // Abnormally high charge amount
        RAPID_CHARGES,        // Multiple charges in short time
        SUSPICIOUS_REFUND,    // Unusual refund patterns
        ACCOUNT_TAKEOVER,     // Suspected account compromise
        SUBSCRIPTION_ABUSE,   // Rapid subscribe/unsubscribe patterns
        VELOCITY_BREACH,      // Transaction velocity exceeded
        GEO_ANOMALY          // Geographic anomaly detected
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AlertStatus {
        ACTIVE, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE
    }
}

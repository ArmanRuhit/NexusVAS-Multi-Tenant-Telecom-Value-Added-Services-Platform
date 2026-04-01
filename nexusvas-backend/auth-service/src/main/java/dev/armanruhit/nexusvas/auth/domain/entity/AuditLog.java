package dev.armanruhit.nexusvas.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_description", length = 500)
    private String eventDescription;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum AuditStatus {
        SUCCESS, FAILURE
    }

    public enum AuditAction {
        LOGIN, LOGOUT, LOGIN_FAILED, TOKEN_ISSUED, TOKEN_REVOKED,
        ROLE_ASSIGNED, ROLE_REVOKED, PERMISSION_DENIED,
        MFA_ENABLED, MFA_DISABLED, MFA_VERIFIED,
        API_KEY_CREATED, API_KEY_REVOKED,
        USER_LOCKED, USER_SUSPENDED, PASSWORD_CHANGED,
        OTP_SENT, OTP_VERIFIED
    }
}

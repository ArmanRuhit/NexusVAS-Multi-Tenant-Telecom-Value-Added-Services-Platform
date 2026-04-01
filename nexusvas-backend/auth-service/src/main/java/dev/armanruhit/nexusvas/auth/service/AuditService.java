package dev.armanruhit.nexusvas.auth.service;

import dev.armanruhit.nexusvas.auth.domain.entity.AuditLog;
import dev.armanruhit.nexusvas.auth.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(UUID userId, String tenantId, AuditLog.AuditAction action,
                    String resourceType, String ipAddress, String userAgent, String description) {
        AuditLog entry = AuditLog.builder()
            .userId(userId)
            .tenantId(tenantId)
            .eventType(action.name())
            .eventDescription(description)
            .resourceType(resourceType)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .status(AuditLog.AuditStatus.SUCCESS)
            .build();
        auditLogRepository.save(entry);
    }

    @Async
    public void log(UUID userId, String tenantId, AuditLog.AuditAction action) {
        log(userId, tenantId, action, null, null, null, null);
    }

    @Async
    public void logFailure(UUID userId, String tenantId, AuditLog.AuditAction action,
                           String errorMessage, String ipAddress, String userAgent) {
        AuditLog entry = AuditLog.builder()
            .userId(userId)
            .tenantId(tenantId)
            .eventType(action.name())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .status(AuditLog.AuditStatus.FAILURE)
            .errorMessage(errorMessage)
            .build();
        auditLogRepository.save(entry);
    }
}

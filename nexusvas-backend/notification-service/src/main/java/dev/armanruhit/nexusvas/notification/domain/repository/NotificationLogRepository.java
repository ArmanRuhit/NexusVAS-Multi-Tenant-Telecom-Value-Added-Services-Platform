package dev.armanruhit.nexusvas.notification.domain.repository;

import dev.armanruhit.nexusvas.notification.domain.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<NotificationLog> findByCorrelationId(UUID correlationId);

    Page<NotificationLog> findByTenantIdAndMsisdnOrderByCreatedAtDesc(String tenantId, String msisdn, Pageable pageable);
}

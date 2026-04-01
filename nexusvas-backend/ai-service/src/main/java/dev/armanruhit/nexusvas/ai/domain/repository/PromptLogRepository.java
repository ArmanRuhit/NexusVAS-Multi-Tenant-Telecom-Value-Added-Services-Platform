package dev.armanruhit.nexusvas.ai.domain.repository;

import dev.armanruhit.nexusvas.ai.domain.entity.PromptLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PromptLogRepository extends JpaRepository<PromptLog, UUID> {

    Page<PromptLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}

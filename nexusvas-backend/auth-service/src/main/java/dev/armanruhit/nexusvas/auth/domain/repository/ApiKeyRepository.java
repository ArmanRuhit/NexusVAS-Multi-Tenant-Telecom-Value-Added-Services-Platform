package dev.armanruhit.nexusvas.auth.domain.repository;

import dev.armanruhit.nexusvas.auth.domain.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    @Query("SELECT k FROM ApiKey k WHERE k.tenantId = :tenantId AND k.status = dev.armanruhit.nexusvas.auth.domain.entity.ApiKey.ApiKeyStatus.ACTIVE")
    List<ApiKey> findByTenantIdAndStatusActive(@Param("tenantId") String tenantId);
}

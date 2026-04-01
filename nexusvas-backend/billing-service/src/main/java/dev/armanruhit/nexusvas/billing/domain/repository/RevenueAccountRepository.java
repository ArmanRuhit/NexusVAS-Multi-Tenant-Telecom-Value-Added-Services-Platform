package dev.armanruhit.nexusvas.billing.domain.repository;

import dev.armanruhit.nexusvas.billing.domain.entity.RevenueAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RevenueAccountRepository extends JpaRepository<RevenueAccount, UUID> {

    Optional<RevenueAccount> findByTenantIdAndAccountCode(String tenantId, String accountCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RevenueAccount r WHERE r.tenantId = :tenantId AND r.accountCode = :code")
    Optional<RevenueAccount> findAndLockByTenantIdAndCode(
        @Param("tenantId") String tenantId,
        @Param("code") String code);
}

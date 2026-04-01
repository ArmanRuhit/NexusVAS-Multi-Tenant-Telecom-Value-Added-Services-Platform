package dev.armanruhit.nexusvas.billing.domain.repository;

import dev.armanruhit.nexusvas.billing.domain.entity.BillingAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, UUID> {

    Optional<BillingAccount> findByTenantIdAndReferenceId(String tenantId, String referenceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BillingAccount b WHERE b.tenantId = :tenantId AND b.referenceId = :referenceId")
    Optional<BillingAccount> findAndLockByTenantIdAndReferenceId(
        @Param("tenantId") String tenantId,
        @Param("referenceId") String referenceId);
}

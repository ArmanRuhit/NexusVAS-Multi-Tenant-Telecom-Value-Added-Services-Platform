package dev.armanruhit.nexusvas.subscription.domain.repository;

import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionProjectionRepository extends JpaRepository<SubscriptionProjection, UUID> {

    Optional<SubscriptionProjection> findBySubscriptionId(UUID subscriptionId);

    Optional<SubscriptionProjection> findByTenantIdAndMsisdnAndProductIdAndStatus(
        String tenantId, String msisdn, UUID productId,
        SubscriptionProjection.SubscriptionStatus status);

    Page<SubscriptionProjection> findByTenantId(String tenantId, Pageable pageable);

    Page<SubscriptionProjection> findByTenantIdAndStatus(
        String tenantId, SubscriptionProjection.SubscriptionStatus status, Pageable pageable);

    List<SubscriptionProjection> findByTenantIdAndMsisdn(String tenantId, String msisdn);

    @Query("""
        SELECT s FROM SubscriptionProjection s
        WHERE s.status = 'ACTIVE'
          AND s.nextBillingAt <= :cutoff
        ORDER BY s.nextBillingAt ASC
        """)
    List<SubscriptionProjection> findDueForRenewal(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("""
        SELECT s FROM SubscriptionProjection s
        WHERE s.tenantId = :tenantId
          AND s.msisdn = :msisdn
          AND s.productId = :productId
          AND s.status = 'ACTIVE'
        """)
    Optional<SubscriptionProjection> findActiveSubscription(
        @Param("tenantId") String tenantId,
        @Param("msisdn") String msisdn,
        @Param("productId") UUID productId);
}

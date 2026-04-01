package dev.armanruhit.nexusvas.subscription.domain.repository;

import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, UUID> {

    List<SubscriptionEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);

    @Query("SELECT MAX(e.version) FROM SubscriptionEvent e WHERE e.aggregateId = :aggregateId")
    Optional<Integer> findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);

    boolean existsByAggregateIdAndVersion(String aggregateId, int version);

    List<SubscriptionEvent> findByTenantIdAndAggregateId(String tenantId, String aggregateId);
}

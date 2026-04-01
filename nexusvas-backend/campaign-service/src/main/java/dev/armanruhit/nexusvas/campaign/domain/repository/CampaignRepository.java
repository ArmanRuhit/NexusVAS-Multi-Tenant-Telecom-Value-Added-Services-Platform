package dev.armanruhit.nexusvas.campaign.domain.repository;

import dev.armanruhit.nexusvas.campaign.domain.entity.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Page<Campaign> findByTenantId(String tenantId, Pageable pageable);

    Page<Campaign> findByTenantIdAndStatus(String tenantId, Campaign.CampaignStatus status, Pageable pageable);

    Optional<Campaign> findByIdAndTenantId(UUID id, String tenantId);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'SCHEDULED' AND c.startAt <= :now")
    List<Campaign> findScheduledCampaignsDue(@Param("now") Instant now);
}

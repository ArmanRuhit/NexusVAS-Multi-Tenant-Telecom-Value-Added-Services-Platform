package dev.armanruhit.nexusvas.campaign.domain.repository;

import dev.armanruhit.nexusvas.campaign.domain.entity.CampaignBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignBatchRepository extends JpaRepository<CampaignBatch, UUID> {

    List<CampaignBatch> findByCampaignIdOrderByBatchNumberAsc(UUID campaignId);

    List<CampaignBatch> findByCampaignIdAndStatus(UUID campaignId, CampaignBatch.BatchStatus status);
}

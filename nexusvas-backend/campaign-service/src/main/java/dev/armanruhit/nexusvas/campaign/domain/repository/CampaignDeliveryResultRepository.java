package dev.armanruhit.nexusvas.campaign.domain.repository;

import dev.armanruhit.nexusvas.campaign.domain.entity.CampaignDeliveryResult;
import dev.armanruhit.nexusvas.campaign.domain.entity.CampaignDeliveryResult.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CampaignDeliveryResultRepository extends JpaRepository<CampaignDeliveryResult, UUID> {

    Page<CampaignDeliveryResult> findByCampaignId(UUID campaignId, Pageable pageable);

    List<CampaignDeliveryResult> findByCampaignIdAndDeliveryStatus(UUID campaignId, DeliveryStatus status);

    List<CampaignDeliveryResult> findByBatchId(UUID batchId);

    long countByCampaignIdAndDeliveryStatus(UUID campaignId, DeliveryStatus status);

    @Query("SELECT COUNT(r) FROM CampaignDeliveryResult r WHERE r.campaign.id = :campaignId AND r.sentAt IS NOT NULL")
    long countSentByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT COUNT(r) FROM CampaignDeliveryResult r WHERE r.campaign.id = :campaignId AND r.deliveredAt IS NOT NULL")
    long countDeliveredByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT COUNT(r) FROM CampaignDeliveryResult r WHERE r.campaign.id = :campaignId AND r.clickedAt IS NOT NULL")
    long countClickedByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT COUNT(r) FROM CampaignDeliveryResult r WHERE r.campaign.id = :campaignId AND r.convertedAt IS NOT NULL")
    long countConvertedByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT new map(r.deliveryStatus as status, COUNT(r) as count) " +
           "FROM CampaignDeliveryResult r WHERE r.campaign.id = :campaignId GROUP BY r.deliveryStatus")
    List<Object[]> getStatusBreakdown(@Param("campaignId") UUID campaignId);
}

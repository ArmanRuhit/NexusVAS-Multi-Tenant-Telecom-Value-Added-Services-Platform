package dev.armanruhit.nexusvas.campaign.service;

import dev.armanruhit.nexusvas.campaign.config.RabbitMQConfig;
import dev.armanruhit.nexusvas.campaign.domain.entity.Campaign;
import dev.armanruhit.nexusvas.campaign.domain.entity.CampaignBatch;
import dev.armanruhit.nexusvas.campaign.domain.repository.CampaignBatchRepository;
import dev.armanruhit.nexusvas.campaign.domain.repository.CampaignRepository;
import dev.armanruhit.nexusvas.campaign.dto.CampaignCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

    private static final int BATCH_SIZE = 500;

    private final CampaignRepository campaignRepository;
    private final CampaignBatchRepository batchRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Campaign create(CampaignCreateRequest req, String tenantId, String createdBy) {
        Campaign campaign = Campaign.builder()
            .tenantId(tenantId)
            .name(req.name())
            .description(req.description())
            .type(req.type())
            .targetCriteria(req.targetCriteria())
            .contentTemplate(req.contentTemplate())
            .startAt(req.startAt())
            .endAt(req.endAt())
            .createdBy(createdBy)
            .build();

        Campaign saved = campaignRepository.save(campaign);
        log.info("Created campaign {} '{}' for tenant {}", saved.getId(), saved.getName(), tenantId);
        return saved;
    }

    public Page<Campaign> list(String tenantId, Campaign.CampaignStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status != null) {
            return campaignRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        }
        return campaignRepository.findByTenantId(tenantId, pageable);
    }

    public Campaign findById(String id, String tenantId) {
        return campaignRepository.findByIdAndTenantId(UUID.fromString(id), tenantId)
            .orElseThrow(() -> new CampaignNotFoundException(id));
    }

    @Transactional
    public Campaign updateStatus(String id, Campaign.CampaignStatus newStatus, String tenantId) {
        Campaign campaign = findById(id, tenantId);
        campaign.setStatus(newStatus);

        if (newStatus == Campaign.CampaignStatus.RUNNING) {
            dispatchCampaignBatches(campaign);
        }

        return campaignRepository.save(campaign);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        Campaign campaign = findById(id, tenantId);
        campaign.setStatus(Campaign.CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
    }

    /** Scheduled: auto-launch campaigns whose startAt has arrived */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void launchScheduledCampaigns() {
        List<Campaign> due = campaignRepository.findScheduledCampaignsDue(Instant.now());
        for (Campaign c : due) {
            log.info("Auto-launching scheduled campaign {}", c.getId());
            c.setStatus(Campaign.CampaignStatus.RUNNING);
            campaignRepository.save(c);
            dispatchCampaignBatches(c);
        }
    }

    // ── Batch dispatch ────────────────────────────────────────────────────────

    /**
     * Splits campaign into batches of BATCH_SIZE and enqueues each to RabbitMQ.
     * Actual subscriber list resolution happens in the notification-service consumer.
     */
    private void dispatchCampaignBatches(Campaign campaign) {
        // Estimate total targets from criteria (actual count resolved by notification-service)
        int estimatedTargets = (int) campaign.getTargetCriteria()
            .getOrDefault("estimatedCount", 1000);

        int totalBatches = (int) Math.ceil((double) estimatedTargets / BATCH_SIZE);
        if (totalBatches == 0) totalBatches = 1;

        for (int i = 0; i < totalBatches; i++) {
            CampaignBatch batch = CampaignBatch.builder()
                .campaign(campaign)
                .batchNumber(i + 1)
                .targetCount(Math.min(BATCH_SIZE, estimatedTargets - i * BATCH_SIZE))
                .build();
            CampaignBatch savedBatch = batchRepository.save(batch);

            Map<String, Object> message = Map.of(
                "campaignId",       campaign.getId().toString(),
                "batchId",          savedBatch.getId().toString(),
                "batchNumber",      i + 1,
                "tenantId",         campaign.getTenantId(),
                "campaignType",     campaign.getType().name(),
                "targetCriteria",   campaign.getTargetCriteria(),
                "contentTemplate",  campaign.getContentTemplate(),
                "offset",           i * BATCH_SIZE,
                "limit",            BATCH_SIZE
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CAMPAIGN_EXCHANGE,
                RabbitMQConfig.CAMPAIGN_DISPATCH_RK,
                message
            );
        }

        campaign.setTotalTargeted(estimatedTargets);
        log.info("Dispatched {} batches for campaign {}", totalBatches, campaign.getId());
    }

    public static class CampaignNotFoundException extends RuntimeException {
        public CampaignNotFoundException(String id) { super("Campaign not found: " + id); }
    }
}

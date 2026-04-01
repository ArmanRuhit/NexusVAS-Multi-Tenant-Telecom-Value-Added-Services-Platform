package dev.armanruhit.nexusvas.notification.messaging;

import dev.armanruhit.nexusvas.notification.config.RabbitMQConfig;
import dev.armanruhit.nexusvas.notification.service.SmsDispatchService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes campaign batch dispatch tasks from RabbitMQ.
 * Resolves subscriber list from targetCriteria and sends notifications.
 * Failed messages are routed to DLQ after exhausting retries.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CampaignBatchConsumer {

    private final SmsDispatchService smsDispatchService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.CAMPAIGN_DISPATCH_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void processBatch(Map<String, Object> message) {
        String campaignId = (String) message.get("campaignId");
        String batchId    = (String) message.get("batchId");
        String tenantId   = (String) message.get("tenantId");
        String type       = (String) message.get("campaignType");

        log.info("Processing campaign batch batchId={} campaignId={} type={}", batchId, campaignId, type);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> contentTemplate = (Map<String, Object>) message.get("contentTemplate");
            @SuppressWarnings("unchecked")
            Map<String, Object> targetCriteria  = (Map<String, Object>) message.get("targetCriteria");

            String content = resolveContent(contentTemplate, type);

            // Resolve subscriber list — in production this queries subscriber-service
            List<String> msisdns = resolveTargetMsisdns(tenantId, targetCriteria);

            UUID correlationId = UUID.randomUUID();
            for (String msisdn : msisdns) {
                smsDispatchService.dispatch(tenantId, msisdn, content, correlationId, "campaign");
            }

            log.info("Dispatched {} notifications for batch {}", msisdns.size(), batchId);
        } catch (Exception e) {
            log.error("Campaign batch processing failed for batchId={}: {}", batchId, e.getMessage());
            // Throwing re-queues to DLQ after max retries
            throw new RuntimeException("Batch processing failed: " + e.getMessage(), e);
        }
    }

    private String resolveContent(Map<String, Object> template, String campaignType) {
        Object body = template.get("body");
        return body != null ? body.toString() : "You have a new message.";
    }

    /**
     * Stub — real implementation queries subscription-service for active subscribers
     * matching the given targetCriteria (productId, segment, region, etc.)
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveTargetMsisdns(String tenantId, Map<String, Object> criteria) {
        Object msisdns = criteria.get("msisdns");
        if (msisdns instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}

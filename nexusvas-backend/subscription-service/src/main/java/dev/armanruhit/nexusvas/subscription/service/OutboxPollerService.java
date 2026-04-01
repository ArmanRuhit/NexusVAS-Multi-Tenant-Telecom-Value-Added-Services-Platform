package dev.armanruhit.nexusvas.subscription.service;

import dev.armanruhit.nexusvas.subscription.domain.entity.OutboxEvent;
import dev.armanruhit.nexusvas.subscription.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls the outbox table and publishes pending events to Kafka.
 * Guarantees at-least-once delivery: if Kafka is unavailable, events stay PENDING
 * and are retried on the next poll cycle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxPollerService {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "subscription-events";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 2000) // poll every 2 seconds
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxRepository.findPendingEvents(
            PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pending) {
            publish(event);
        }

        // Also retry failed events
        List<OutboxEvent> retryable = outboxRepository.findRetryableEvents(
            PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : retryable) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        markFailed(event, ex.getMessage());
                    } else {
                        markPublished(event);
                    }
                });
        } catch (Exception e) {
            markFailed(event, e.getMessage());
        }
    }

    private void markPublished(OutboxEvent event) {
        event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        outboxRepository.save(event);
    }

    private void markFailed(OutboxEvent event, String error) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(error);
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.setStatus(OutboxEvent.OutboxStatus.FAILED);
            log.error("Outbox event {} permanently failed after {} retries: {}",
                event.getId(), MAX_RETRIES, error);
        }
        outboxRepository.save(event);
    }
}

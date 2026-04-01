package dev.armanruhit.nexusvas.subscription.messaging;

import dev.armanruhit.nexusvas.subscription.service.SubscriptionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes billing events from Kafka to drive subscription state transitions.
 * Implements the Saga pattern: subscription becomes ACTIVE only after billing confirms.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BillingEventConsumer {

    private final SubscriptionCommandService commandService;

    @KafkaListener(topics = "billing-events", groupId = "subscription-billing-saga-group")
    public void onBillingEvent(@Payload Map<String, Object> event,
                                @Header(value = "eventType", required = false) String eventType) {
        if (eventType == null) {
            eventType = (String) event.get("eventType");
        }

        switch (eventType != null ? eventType : "") {
            case "ChargeSucceeded" -> handleChargeSucceeded(event);
            case "ChargeFailed"    -> handleChargeFailed(event);
            default -> log.debug("Ignoring billing event type: {}", eventType);
        }
    }

    private void handleChargeSucceeded(Map<String, Object> event) {
        try {
            UUID subscriptionId = UUID.fromString((String) event.get("subscriptionId"));
            String tenantId = (String) event.get("tenantId");
            commandService.activateSubscription(subscriptionId, tenantId);
            log.info("Subscription {} activated after successful charge", subscriptionId);
        } catch (Exception e) {
            log.error("Failed to activate subscription on ChargeSucceeded: {}", e.getMessage(), e);
            throw e; // Kafka will retry
        }
    }

    private void handleChargeFailed(Map<String, Object> event) {
        try {
            UUID subscriptionId = UUID.fromString((String) event.get("subscriptionId"));
            String tenantId = (String) event.get("tenantId");
            String reason = (String) event.getOrDefault("failureReason", "Billing charge failed");
            commandService.cancelSubscription(subscriptionId, tenantId, reason);
            log.warn("Subscription {} cancelled due to charge failure", subscriptionId);
        } catch (Exception e) {
            log.error("Failed to cancel subscription on ChargeFailed: {}", e.getMessage(), e);
            throw e;
        }
    }
}

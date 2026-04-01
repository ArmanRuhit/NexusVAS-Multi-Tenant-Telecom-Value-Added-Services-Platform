package dev.armanruhit.nexusvas.notification.messaging;

import dev.armanruhit.nexusvas.notification.config.RabbitMQConfig;
import dev.armanruhit.nexusvas.notification.service.SmsDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes subscription events from RabbitMQ fanout exchange.
 * Sends transactional notifications: welcome SMS, renewal confirmation, cancellation notice.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final SmsDispatchService smsDispatchService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void onSubscriptionEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String tenantId  = (String) event.get("tenantId");
        String msisdn    = (String) event.get("msisdn");

        log.info("Received subscription event type={} tenant={} msisdn={}", eventType, tenantId, msisdn);

        try {
            String content = buildNotificationContent(eventType, event);
            if (content != null) {
                smsDispatchService.dispatch(
                    tenantId,
                    msisdn,
                    content,
                    UUID.randomUUID(),
                    "transactional"
                );
                log.info("Sent transactional SMS for event {} to {}", eventType, msisdn);
            }
        } catch (Exception e) {
            log.error("Failed to process subscription event {}: {}", eventType, e.getMessage());
            throw new RuntimeException("Event processing failed: " + e.getMessage(), e);
        }
    }

    private String buildNotificationContent(String eventType, Map<String, Object> event) {
        String productName = (String) event.getOrDefault("productName", "our service");
        
        return switch (eventType) {
            case "SubscriptionCreated" -> 
                "Welcome to %s! You have been successfully subscribed. Thank you for joining.".formatted(productName);
            case "SubscriptionRenewed" -> 
                "Your %s subscription has been renewed. Thank you for staying with us!".formatted(productName);
            case "SubscriptionCancelled" -> 
                "Your %s subscription has been cancelled. You will no longer be charged.".formatted(productName);
            case "SubscriptionExpired" -> 
                "Your %s subscription has expired. Resubscribe to continue enjoying our services!".formatted(productName);
            default -> null;
        };
    }
}

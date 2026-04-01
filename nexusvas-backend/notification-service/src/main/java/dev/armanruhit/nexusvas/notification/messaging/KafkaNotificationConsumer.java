package dev.armanruhit.nexusvas.notification.messaging;

import dev.armanruhit.nexusvas.notification.service.SmsDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes OTP and system notification events from Kafka "notification-events" topic.
 * Campaign bulk dispatch is handled by RabbitMQ (CampaignBatchConsumer).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationConsumer {

    private final SmsDispatchService smsDispatchService;

    @KafkaListener(topics = "notification-events", groupId = "notification-kafka-group")
    public void consume(@Payload Map<String, Object> event,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        String eventType = (String) event.get("eventType");
        log.debug("Received notification event type={}", eventType);

        switch (eventType != null ? eventType : "") {
            case "OtpRequested" -> handleOtp(event);
            case "SubscriptionActivated" -> handleSubscriptionActivated(event);
            case "ChargeSucceeded" -> handleChargeSucceeded(event);
            default -> log.debug("Ignoring event type={}", eventType);
        }
    }

    private void handleOtp(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        String msisdn   = (String) event.get("msisdn");
        String otp      = (String) event.get("otp");
        if (msisdn == null || otp == null) return;

        String content = "Your OTP is: " + otp + ". Valid for 5 minutes. Do not share.";
        smsDispatchService.dispatch(tenantId, msisdn, content,
            UUID.randomUUID(), "system");
    }

    private void handleSubscriptionActivated(Map<String, Object> event) {
        String tenantId    = (String) event.get("tenantId");
        String msisdn      = (String) event.get("msisdn");
        String productName = (String) event.get("productName");
        if (msisdn == null) return;

        String content = "You have successfully subscribed to " + productName + ". Enjoy!";
        smsDispatchService.dispatch(tenantId, msisdn, content,
            UUID.randomUUID(), "system");
    }

    private void handleChargeSucceeded(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        String msisdn   = (String) event.get("msisdn");
        Object amount   = event.get("amount");
        if (msisdn == null) return;

        String content = "Your subscription has been renewed. Amount charged: " + amount;
        smsDispatchService.dispatch(tenantId, msisdn, content,
            UUID.randomUUID(), "system");
    }
}

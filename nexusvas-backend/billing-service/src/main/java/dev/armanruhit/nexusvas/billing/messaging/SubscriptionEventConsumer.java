package dev.armanruhit.nexusvas.billing.messaging;

import dev.armanruhit.nexusvas.billing.dto.ChargeRequest;
import dev.armanruhit.nexusvas.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes subscription events from Kafka to trigger billing charges.
 * SubscriptionCreated → initiate charge → publish ChargeSucceeded / ChargeFailed
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final BillingService billingService;

    @KafkaListener(topics = "subscription-events", groupId = "billing-subscription-group")
    public void onSubscriptionEvent(@Payload Map<String, Object> event,
                                    @Header(value = "eventType", required = false) String eventType) {
        if (eventType == null) {
            eventType = (String) event.get("eventType");
        }

        switch (eventType != null ? eventType : "") {
            case "SubscriptionCreated" -> handleSubscriptionCreated(event);
            case "SubscriptionRenewed"  -> handleSubscriptionRenewed(event);
            default -> log.debug("Billing: ignoring event type {}", eventType);
        }
    }

    private void handleSubscriptionCreated(Map<String, Object> event) {
        ChargeRequest request = buildChargeRequest(event);
        // Ensure account exists before charging
        billingService.getOrCreateSubscriberAccount(
            request.tenantId(), request.msisdn(), request.currency());
        billingService.charge(request);
        log.info("Processed billing for new subscription {}", request.subscriptionId());
    }

    private void handleSubscriptionRenewed(Map<String, Object> event) {
        ChargeRequest request = buildChargeRequest(event);
        billingService.charge(request);
        log.info("Processed renewal billing for subscription {}", request.subscriptionId());
    }

    private ChargeRequest buildChargeRequest(Map<String, Object> event) {
        return new ChargeRequest(
            (String) event.get("tenantId"),
            (String) event.get("msisdn"),
            UUID.fromString((String) event.get("subscriptionId")),
            (String) event.getOrDefault("productName", "VAS Product"),
            new BigDecimal(event.getOrDefault("priceAmount", "0").toString()),
            (String) event.getOrDefault("priceCurrency", "BDT"),
            LocalDate.now().toString() // billing cycle date for idempotency
        );
    }
}

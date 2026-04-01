package dev.armanruhit.nexusvas.analytics.messaging;

import dev.armanruhit.nexusvas.analytics.domain.document.BillingEvent;
import dev.armanruhit.nexusvas.analytics.domain.document.SubscriptionEvent;
import dev.armanruhit.nexusvas.analytics.domain.repository.BillingEventRepository;
import dev.armanruhit.nexusvas.analytics.domain.repository.SubscriptionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventIngestionConsumer {

    private static final Set<String> SUBSCRIPTION_EVENT_TYPES = Set.of(
        "SubscriptionCreated", "SubscriptionActivated", "SubscriptionCancelled",
        "SubscriptionRenewed", "SubscriptionExpired"
    );

    private static final Set<String> BILLING_EVENT_TYPES = Set.of(
        "ChargeSucceeded", "ChargeFailed", "RefundIssued"
    );

    private final SubscriptionEventRepository subscriptionRepo;
    private final BillingEventRepository billingRepo;

    @KafkaListener(topics = "subscription-events", groupId = "analytics-subscription-group")
    public void consumeSubscriptionEvent(@Payload Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (eventType == null || !SUBSCRIPTION_EVENT_TYPES.contains(eventType)) return;

        SubscriptionEvent doc = SubscriptionEvent.builder()
            .tenantId((String) event.get("tenantId"))
            .eventType(eventType)
            .msisdn((String) event.get("msisdn"))
            .productId((String) event.get("productId"))
            .productName((String) event.get("productName"))
            .aggregateId((String) event.get("aggregateId"))
            .timestamp(parseInstant(event.get("timestamp")))
            .payload(event)
            .build();

        subscriptionRepo.save(doc);
        log.debug("Ingested subscription event type={} tenantId={}", eventType, doc.getTenantId());
    }

    @KafkaListener(topics = "billing-events", groupId = "analytics-billing-group")
    public void consumeBillingEvent(@Payload Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (eventType == null || !BILLING_EVENT_TYPES.contains(eventType)) return;

        BillingEvent doc = BillingEvent.builder()
            .tenantId((String) event.get("tenantId"))
            .eventType(eventType)
            .msisdn((String) event.get("msisdn"))
            .productId((String) event.get("productId"))
            .subscriptionId((String) event.get("subscriptionId"))
            .amount(parseBigDecimal(event.get("amount")))
            .currency((String) event.get("currency"))
            .transactionId((String) event.get("transactionId"))
            .timestamp(parseInstant(event.get("timestamp")))
            .build();

        billingRepo.save(doc);
        log.debug("Ingested billing event type={} tenantId={}", eventType, doc.getTenantId());
    }

    private Instant parseInstant(Object val) {
        if (val == null) return Instant.now();
        if (val instanceof Instant i) return i;
        try { return Instant.parse(val.toString()); } catch (Exception e) { return Instant.now(); }
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}

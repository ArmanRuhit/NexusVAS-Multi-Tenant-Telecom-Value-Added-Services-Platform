package dev.armanruhit.nexusvas.analytics.domain.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "analytics_billing_events")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_time", def = "{'tenantId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "tenant_product_time", def = "{'tenantId': 1, 'productId': 1, 'timestamp': -1}")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEvent {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String eventType;   // ChargeSucceeded, ChargeFailed, RefundIssued
    private String msisdn;
    private String productId;
    private String subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String transactionId;

    @Indexed
    private Instant timestamp;
}

package dev.armanruhit.nexusvas.analytics.domain.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "analytics_subscription_events")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_product", def = "{'tenantId': 1, 'productId': 1}"),
    @CompoundIndex(name = "tenant_event_time", def = "{'tenantId': 1, 'eventType': 1, 'timestamp': -1}")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEvent {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String eventType;   // SubscriptionCreated, SubscriptionActivated, SubscriptionCancelled, SubscriptionRenewed
    private String msisdn;
    private String productId;
    private String productName;
    private String aggregateId;

    @Indexed
    private Instant timestamp;

    private Map<String, Object> payload;
}

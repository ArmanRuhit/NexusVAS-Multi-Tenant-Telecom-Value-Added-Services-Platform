package dev.armanruhit.nexusvas.analytics.domain.repository;

import dev.armanruhit.nexusvas.analytics.domain.document.SubscriptionEvent;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SubscriptionEventRepository extends MongoRepository<SubscriptionEvent, String> {

    long countByTenantIdAndEventTypeAndTimestampBetween(
        String tenantId, String eventType, Instant from, Instant to);

    @Aggregation(pipeline = {
        "{ $match: { tenantId: ?0, timestamp: { $gte: ?1, $lte: ?2 } } }",
        "{ $group: { _id: { productId: '$productId', eventType: '$eventType' }, count: { $sum: 1 } } }",
        "{ $sort: { count: -1 } }"
    })
    List<ProductEventCount> countByProduct(String tenantId, Instant from, Instant to);

    record ProductEventCount(String productId, String eventType, long count) {}
}

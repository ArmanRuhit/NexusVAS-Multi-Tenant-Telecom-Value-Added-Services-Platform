package dev.armanruhit.nexusvas.analytics.domain.repository;

import dev.armanruhit.nexusvas.analytics.domain.document.BillingEvent;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BillingEventRepository extends MongoRepository<BillingEvent, String> {

    long countByTenantIdAndEventTypeAndTimestampBetween(
        String tenantId, String eventType, Instant from, Instant to);

    @Aggregation(pipeline = {
        "{ $match: { tenantId: ?0, eventType: 'ChargeSucceeded', timestamp: { $gte: ?1, $lte: ?2 } } }",
        "{ $group: { _id: null, total: { $sum: '$amount' } } }"
    })
    BigDecimal sumRevenue(String tenantId, Instant from, Instant to);

    @Aggregation(pipeline = {
        "{ $match: { tenantId: ?0, timestamp: { $gte: ?1, $lte: ?2 } } }",
        "{ $group: { _id: '$productId', revenue: { $sum: '$amount' }, charges: { $sum: 1 } } }",
        "{ $sort: { revenue: -1 } }",
        "{ $limit: 10 }"
    })
    List<ProductRevenue> topProductsByRevenue(String tenantId, Instant from, Instant to);

    record ProductRevenue(String productId, BigDecimal revenue, long charges) {}
}

package dev.armanruhit.nexusvas.analytics.service;

import dev.armanruhit.nexusvas.analytics.domain.repository.BillingEventRepository;
import dev.armanruhit.nexusvas.analytics.domain.repository.SubscriptionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final SubscriptionEventRepository subscriptionRepo;
    private final BillingEventRepository billingRepo;
    private final MongoTemplate mongoTemplate;

    public DashboardSummary getDashboardSummary(String tenantId, int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant to   = Instant.now();

        long newSubscriptions = subscriptionRepo.countByTenantIdAndEventTypeAndTimestampBetween(
            tenantId, "SubscriptionActivated", from, to);
        long cancellations = subscriptionRepo.countByTenantIdAndEventTypeAndTimestampBetween(
            tenantId, "SubscriptionCancelled", from, to);
        long chargesSucceeded = billingRepo.countByTenantIdAndEventTypeAndTimestampBetween(
            tenantId, "ChargeSucceeded", from, to);
        long chargesFailed = billingRepo.countByTenantIdAndEventTypeAndTimestampBetween(
            tenantId, "ChargeFailed", from, to);
        BigDecimal totalRevenue = billingRepo.sumRevenue(tenantId, from, to);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long netSubscriptions = newSubscriptions - cancellations;
        double churnRate = newSubscriptions > 0 ? (double) cancellations / newSubscriptions * 100 : 0;
        double arpu = newSubscriptions > 0 ? totalRevenue.divide(BigDecimal.valueOf(newSubscriptions), 2, RoundingMode.HALF_UP).doubleValue() : 0;

        return new DashboardSummary(newSubscriptions, cancellations,
            chargesSucceeded, chargesFailed, totalRevenue,
            netSubscriptions, Math.round(churnRate * 100.0) / 100.0, arpu);
    }

    public List<BillingEventRepository.ProductRevenue> topProducts(String tenantId, int days, int limit) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        return billingRepo.topProductsByRevenue(tenantId, from, Instant.now())
            .stream().limit(limit).toList();
    }

    public List<SubscriptionEventRepository.ProductEventCount> subscriptionsByProduct(String tenantId, int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        return subscriptionRepo.countByProduct(tenantId, from, Instant.now());
    }

    // ── Time-series analytics ─────────────────────────────────────────────────

    public List<TimeSeriesPoint> revenueTimeline(String tenantId, int days, String granularity) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        List<TimeSeriesPoint> points = new ArrayList<>();
        
        LocalDate start = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            
            BigDecimal revenue = billingRepo.sumRevenue(tenantId, dayStart, dayEnd);
            points.add(new TimeSeriesPoint(
                dayStart.toString(),
                revenue != null ? revenue.doubleValue() : 0.0,
                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ));
        }
        return points;
    }

    public List<SubscriptionTimelinePoint> subscriptionTimeline(String tenantId, int days, String granularity) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        List<SubscriptionTimelinePoint> points = new ArrayList<>();
        
        LocalDate start = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        
        long runningTotal = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            
            long newSubs = subscriptionRepo.countByTenantIdAndEventTypeAndTimestampBetween(
                tenantId, "SubscriptionActivated", dayStart, dayEnd);
            long cancels = subscriptionRepo.countByTenantIdAndEventTypeAndTimestampBetween(
                tenantId, "SubscriptionCancelled", dayStart, dayEnd);
            
            runningTotal += newSubs - cancels;
            points.add(new SubscriptionTimelinePoint(
                dayStart.toString(), newSubs, cancels, newSubs - cancels, runningTotal
            ));
        }
        return points;
    }

    // ── Subscriber analytics ──────────────────────────────────────────────────

    public SubscriberMetrics getSubscriberMetrics(String tenantId) {
        // Aggregate subscriber counts by product
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("tenantId").is(tenantId)),
            Aggregation.group("productId").count().as("count"),
            Aggregation.project("count").and("_id").as("productId")
        );
        
        List<ProductSubscriberCount> byProduct = new ArrayList<>();
        long totalActive = 0;
        
        // Calculate metrics from subscription events
        Instant monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long newThisMonth = subscriptionRepo.countByTenantIdAndEventTypeAndTimestampBetween(
            tenantId, "SubscriptionActivated", monthStart, Instant.now());
        long churnedThisMonth = subscriptionRepo.countByTenantIdAndEventTypeAndTimestampBetween(
            tenantId, "SubscriptionCancelled", monthStart, Instant.now());
        
        return new SubscriberMetrics(
            totalActive, newThisMonth, churnedThisMonth, 0.0, byProduct, List.of()
        );
    }

    public List<ChurnRiskSegment> getChurnRiskSegments(String tenantId) {
        // Placeholder - would integrate with AI Service churn predictions
        return List.of(
            new ChurnRiskSegment("HIGH", 150, 15.0, 30.0, 5.0),
            new ChurnRiskSegment("MEDIUM", 300, 30.0, 90.0, 8.0),
            new ChurnRiskSegment("LOW", 550, 55.0, 180.0, 12.0)
        );
    }

    // ── Campaign analytics ───────────────────────────────────────────────────

    public CampaignPerformance getCampaignPerformance(String tenantId, String campaignId) {
        // Placeholder - would query campaign delivery results
        return new CampaignPerformance(
            campaignId, "Campaign " + campaignId.substring(0, 8),
            10000, 9500, 9000, 1500, 300,
            90.0, 16.67, 3.33, 100.0, 300.0
        );
    }

    public List<CampaignPerformance> getTopCampaigns(String tenantId, int limit) {
        // Placeholder - would query top campaigns by conversion
        return List.of(
            new CampaignPerformance("camp-1", "Summer Promo", 10000, 9500, 9000, 1500, 300, 90.0, 16.67, 3.33, 100.0, 300.0),
            new CampaignPerformance("camp-2", "Flash Sale", 5000, 4800, 4500, 800, 150, 93.75, 17.78, 3.33, 50.0, 200.0)
        );
    }

    // ── Records for GraphQL responses ─────────────────────────────────────────

    public record DashboardSummary(
        long newSubscriptions,
        long cancellations,
        long chargesSucceeded,
        long chargesFailed,
        BigDecimal totalRevenue,
        long netSubscriptions,
        double churnRate,
        double arpu
    ) {}

    public record TimeSeriesPoint(String timestamp, double value, String label) {}

    public record SubscriptionTimelinePoint(
        String timestamp, long newSubscriptions, long cancellations, 
        long netChange, long totalActive
    ) {}

    public record SubscriberMetrics(
        long totalActive, long newThisMonth, long churnedThisMonth,
        double averageTenureDays, List<ProductSubscriberCount> byProduct,
        List<RegionSubscriberCount> byRegion
    ) {}

    public record ProductSubscriberCount(String productId, String productName, long count, double percentage) {}

    public record RegionSubscriberCount(String region, long count, double percentage) {}

    public record ChurnRiskSegment(String riskLevel, long count, double percentage, double avgTenureDays, double avgRevenue) {}

    public record CampaignPerformance(
        String campaignId, String campaignName, long totalTargeted,
        long totalSent, long totalDelivered, long totalClicked, long totalConverted,
        double deliveryRate, double clickRate, double conversionRate,
        double cost, double roi
    ) {}
}

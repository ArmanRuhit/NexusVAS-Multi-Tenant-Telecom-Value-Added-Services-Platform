package dev.armanruhit.nexusvas.analytics.graphql;

import dev.armanruhit.nexusvas.analytics.domain.repository.BillingEventRepository;
import dev.armanruhit.nexusvas.analytics.domain.repository.SubscriptionEventRepository;
import dev.armanruhit.nexusvas.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AnalyticsResolver {

    private final AnalyticsQueryService queryService;

    @QueryMapping
    public AnalyticsQueryService.DashboardSummary dashboardSummary(
            @Argument int days,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.getDashboardSummary(jwt.getClaimAsString("tenant_id"), days);
    }

    @QueryMapping
    public List<BillingEventRepository.ProductRevenue> topProducts(
            @Argument int days,
            @Argument int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.topProducts(jwt.getClaimAsString("tenant_id"), days, limit);
    }

    @QueryMapping
    public List<SubscriptionEventRepository.ProductEventCount> subscriptionsByProduct(
            @Argument int days,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.subscriptionsByProduct(jwt.getClaimAsString("tenant_id"), days);
    }

    @QueryMapping
    public List<AnalyticsQueryService.TimeSeriesPoint> revenueTimeline(
            @Argument int days,
            @Argument String granularity,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.revenueTimeline(jwt.getClaimAsString("tenant_id"), days, granularity);
    }

    @QueryMapping
    public List<AnalyticsQueryService.SubscriptionTimelinePoint> subscriptionTimeline(
            @Argument int days,
            @Argument String granularity,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.subscriptionTimeline(jwt.getClaimAsString("tenant_id"), days, granularity);
    }

    @QueryMapping
    public AnalyticsQueryService.SubscriberMetrics subscriberMetrics(
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.getSubscriberMetrics(jwt.getClaimAsString("tenant_id"));
    }

    @QueryMapping
    public List<AnalyticsQueryService.ChurnRiskSegment> churnRiskSegments(
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.getChurnRiskSegments(jwt.getClaimAsString("tenant_id"));
    }

    @QueryMapping
    public AnalyticsQueryService.CampaignPerformance campaignPerformance(
            @Argument String campaignId,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.getCampaignPerformance(jwt.getClaimAsString("tenant_id"), campaignId);
    }

    @QueryMapping
    public List<AnalyticsQueryService.CampaignPerformance> topCampaigns(
            @Argument int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return queryService.getTopCampaigns(jwt.getClaimAsString("tenant_id"), limit);
    }
}

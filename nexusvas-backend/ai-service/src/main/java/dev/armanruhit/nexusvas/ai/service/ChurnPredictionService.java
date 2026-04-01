package dev.armanruhit.nexusvas.ai.service;

import dev.armanruhit.nexusvas.ai.domain.entity.ChurnScore;
import dev.armanruhit.nexusvas.ai.domain.repository.ChurnScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Churn prediction service.
 * Listens to subscription/billing events and maintains churn risk scores.
 * Scoring model is a heuristic rule engine; replace with ML model as needed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChurnPredictionService {

    private static final String MODEL_VERSION = "heuristic-v1";

    private final ChurnScoreRepository churnScoreRepository;

    public Page<ChurnScore> getHighRiskSubscribers(String tenantId, ChurnScore.RiskLevel riskLevel,
                                                    int page, int size) {
        return churnScoreRepository.findByTenantIdAndRiskLevelOrderByScoreDesc(
            tenantId, riskLevel, PageRequest.of(page, size));
    }

    public List<ChurnScore> getTodayScores(String tenantId) {
        return churnScoreRepository.findByTenantIdAndPredictionDateOrderByScoreDesc(
            tenantId, LocalDate.now());
    }

    @KafkaListener(topics = {"subscription-events", "billing-events"},
                   groupId = "ai-churn-prediction-group")
    @Transactional
    public void onEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        String tenantId  = (String) event.get("tenantId");
        String msisdn    = (String) event.get("msisdn");
        if (msisdn == null || tenantId == null) return;

        LocalDate today = LocalDate.now();

        ChurnScore existing = churnScoreRepository
            .findByTenantIdAndMsisdnAndPredictionDate(tenantId, msisdn, today)
            .orElseGet(() -> ChurnScore.builder()
                .tenantId(tenantId)
                .msisdn(msisdn)
                .score(BigDecimal.valueOf(0.1))
                .riskLevel(ChurnScore.RiskLevel.LOW)
                .modelVersion(MODEL_VERSION)
                .predictionDate(today)
                .build());

        BigDecimal updatedScore = computeScore(existing.getScore(), eventType);
        existing.setScore(updatedScore);
        existing.setRiskLevel(classifyRisk(updatedScore));
        existing.setContributingFactors(Map.of("lastEvent", eventType));

        churnScoreRepository.save(existing);
        log.debug("Updated churn score for msisdn={} score={} risk={}", msisdn, updatedScore, existing.getRiskLevel());
    }

    private BigDecimal computeScore(BigDecimal current, String eventType) {
        double delta = switch (eventType != null ? eventType : "") {
            case "SubscriptionCancelled" -> +0.35;
            case "ChargeFailed"          -> +0.20;
            case "SubscriptionActivated" -> -0.15;
            case "ChargeSucceeded"       -> -0.05;
            case "SubscriptionRenewed"   -> -0.10;
            default                      ->  0.0;
        };
        double updated = Math.max(0.0, Math.min(1.0, current.doubleValue() + delta));
        return BigDecimal.valueOf(updated).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private ChurnScore.RiskLevel classifyRisk(BigDecimal score) {
        double v = score.doubleValue();
        if (v >= 0.75) return ChurnScore.RiskLevel.CRITICAL;
        if (v >= 0.50) return ChurnScore.RiskLevel.HIGH;
        if (v >= 0.25) return ChurnScore.RiskLevel.MEDIUM;
        return ChurnScore.RiskLevel.LOW;
    }
}

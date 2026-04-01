package dev.armanruhit.nexusvas.ai.domain.repository;

import dev.armanruhit.nexusvas.ai.domain.entity.ChurnScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChurnScoreRepository extends JpaRepository<ChurnScore, UUID> {

    Optional<ChurnScore> findByTenantIdAndMsisdnAndPredictionDate(
        String tenantId, String msisdn, LocalDate predictionDate);

    Page<ChurnScore> findByTenantIdAndRiskLevelOrderByScoreDesc(
        String tenantId, ChurnScore.RiskLevel riskLevel, Pageable pageable);

    List<ChurnScore> findByTenantIdAndPredictionDateOrderByScoreDesc(
        String tenantId, LocalDate predictionDate);
}

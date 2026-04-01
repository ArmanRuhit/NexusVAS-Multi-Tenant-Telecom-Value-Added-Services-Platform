package dev.armanruhit.nexusvas.ai.domain.repository;

import dev.armanruhit.nexusvas.ai.domain.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    List<FraudAlert> findByTenantIdAndStatusOrderByDetectedAtDesc(
        String tenantId, FraudAlert.AlertStatus status);

    List<FraudAlert> findByTenantIdAndMsisdnOrderByDetectedAtDesc(
        String tenantId, String msisdn);

    List<FraudAlert> findByTenantIdAndSeverityOrderByDetectedAtDesc(
        String tenantId, FraudAlert.Severity severity);

    long countByTenantIdAndStatus(String tenantId, FraudAlert.AlertStatus status);
}

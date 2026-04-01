package dev.armanruhit.nexusvas.operator.domain.repository;

import dev.armanruhit.nexusvas.operator.domain.entity.OperatorConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperatorConfigRepository extends JpaRepository<OperatorConfig, UUID> {

    List<OperatorConfig> findByOperatorId(UUID operatorId);

    Optional<OperatorConfig> findByOperatorIdAndConfigKey(UUID operatorId, String configKey);
}

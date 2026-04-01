package dev.armanruhit.nexusvas.operator.domain.repository;

import dev.armanruhit.nexusvas.operator.domain.entity.OperatorApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OperatorApiKeyRepository extends JpaRepository<OperatorApiKey, UUID> {

    List<OperatorApiKey> findByOperatorId(UUID operatorId);

    java.util.Optional<OperatorApiKey> findByPrefix(String prefix);
}

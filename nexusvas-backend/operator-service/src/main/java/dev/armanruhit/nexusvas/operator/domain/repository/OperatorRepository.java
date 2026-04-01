package dev.armanruhit.nexusvas.operator.domain.repository;

import dev.armanruhit.nexusvas.operator.domain.entity.Operator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperatorRepository extends JpaRepository<Operator, UUID> {

    Optional<Operator> findBySlug(String slug);

    Page<Operator> findByStatus(Operator.OperatorStatus status, Pageable pageable);

    boolean existsBySlug(String slug);
}

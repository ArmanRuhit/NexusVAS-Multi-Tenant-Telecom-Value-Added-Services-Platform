package dev.armanruhit.nexusvas.billing.domain.repository;

import dev.armanruhit.nexusvas.billing.domain.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Page<LedgerEntry> findByTenantIdAndAccountId(String tenantId, UUID accountId, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
        FROM LedgerEntry e
        WHERE e.accountId = :accountId
          AND e.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumNetByAccountAndPeriod(
        @Param("accountId") UUID accountId,
        @Param("from") Instant from,
        @Param("to") Instant to);
}

package dev.armanruhit.nexusvas.campaign.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_batches")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "batch_number", nullable = false)
    private int batchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BatchStatus status = BatchStatus.PENDING;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "sent_count")
    @Builder.Default
    private int sentCount = 0;

    @Column(name = "delivered_count")
    @Builder.Default
    private int deliveredCount = 0;

    @Column(name = "failed_count")
    @Builder.Default
    private int failedCount = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum BatchStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}

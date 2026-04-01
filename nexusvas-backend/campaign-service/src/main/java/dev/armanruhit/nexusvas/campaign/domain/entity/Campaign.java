package dev.armanruhit.nexusvas.campaign.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false)
    private CampaignType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_criteria", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> targetCriteria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_template", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contentTemplate;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "total_targeted")
    @Builder.Default
    private int totalTargeted = 0;

    @Column(name = "total_sent")
    @Builder.Default
    private int totalSent = 0;

    @Column(name = "total_delivered")
    @Builder.Default
    private int totalDelivered = 0;

    @Column(name = "total_clicked")
    @Builder.Default
    private int totalClicked = 0;

    @Column(name = "total_converted")
    @Builder.Default
    private int totalConverted = 0;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum CampaignType { PUSH, IN_APP, SMS, MULTI_CHANNEL }

    public enum CampaignStatus { DRAFT, SCHEDULED, RUNNING, PAUSED, COMPLETED, CANCELLED }
}

package dev.armanruhit.nexusvas.campaign.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks individual delivery status for each subscriber in a campaign.
 * Used for campaign performance analytics and conversion tracking.
 */
@Entity
@Table(name = "campaign_delivery_results", 
       indexes = {
           @Index(name = "idx_delivery_campaign", columnList = "campaign_id"),
           @Index(name = "idx_delivery_msisdn", columnList = "msisdn"),
           @Index(name = "idx_delivery_status", columnList = "delivery_status")
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignDeliveryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum DeliveryStatus {
        PENDING,      // Queued for dispatch
        SENT,         // Handed off to SMS gateway
        DELIVERED,    // Confirmed delivered to handset
        FAILED,       // Delivery failed
        CLICKED,      // User clicked link (if applicable)
        CONVERTED     // User subscribed after campaign
    }
}

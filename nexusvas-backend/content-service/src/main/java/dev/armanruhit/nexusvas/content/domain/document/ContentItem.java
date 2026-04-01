package dev.armanruhit.nexusvas.content.domain.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "content_items")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tenant_status_type",    def = "{'tenantId':1,'status':1,'type':1}"),
    @CompoundIndex(name = "idx_tenant_products_status", def = "{'tenantId':1,'targetProducts':1,'status':1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItem {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private ContentType type;

    @TextIndexed(weight = 3)
    private String title;

    @TextIndexed
    private String description;

    @TextIndexed
    private List<String> tags;

    private String language;

    private ContentStatus status;

    private ContentVisibility visibility;

    private List<String> targetProducts;

    private String thumbnailUrl;

    @Indexed(expireAfterSeconds = 0) // MongoDB TTL — auto-deletes when expiresAt passes
    private Instant expiresAt;

    private Instant publishedAt;

    private Map<String, Object> metadata; // type-specific fields

    private String createdBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ContentType {
        GAME, HEALTH_TIP, VIDEO, STICKER_PACK, ARTICLE
    }

    public enum ContentStatus {
        DRAFT, PUBLISHED, ARCHIVED, EXPIRED
    }

    public enum ContentVisibility {
        TENANT_ONLY, GLOBAL
    }
}

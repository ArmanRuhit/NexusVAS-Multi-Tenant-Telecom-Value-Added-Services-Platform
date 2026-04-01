package dev.armanruhit.nexusvas.content.service;

import dev.armanruhit.nexusvas.content.domain.document.ContentItem;
import dev.armanruhit.nexusvas.content.domain.repository.ContentItemRepository;
import dev.armanruhit.nexusvas.content.dto.ContentCreateRequest;
import dev.armanruhit.nexusvas.content.dto.ContentUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentCatalogService {

    private final ContentItemRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<ContentItem> create(ContentCreateRequest req, String tenantId, String createdBy) {
        ContentItem item = ContentItem.builder()
            .tenantId(tenantId)
            .type(req.type())
            .title(req.title())
            .description(req.description())
            .tags(req.tags())
            .language(req.language())
            .status(ContentItem.ContentStatus.DRAFT)
            .visibility(req.visibility() != null ? req.visibility() : ContentItem.ContentVisibility.TENANT_ONLY)
            .targetProducts(req.targetProducts())
            .thumbnailUrl(req.thumbnailUrl())
            .expiresAt(req.expiresAt())
            .metadata(req.metadata())
            .createdBy(createdBy)
            .build();

        return repository.save(item)
            .doOnSuccess(saved -> log.info("Created content {} '{}' for tenant {}", saved.getId(), saved.getTitle(), tenantId));
    }

    public Flux<ContentItem> list(String tenantId, ContentItem.ContentStatus status,
                                  ContentItem.ContentType type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (type != null) {
            return repository.findByTenantIdAndStatusAndType(tenantId, status, type, pageable);
        }
        return repository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    public Mono<ContentItem> findById(String id, String tenantId) {
        return repository.findById(id)
            .filter(item -> item.getTenantId().equals(tenantId));
    }

    public Mono<ContentItem> update(String id, ContentUpdateRequest req, String tenantId) {
        return repository.findById(id)
            .filter(item -> item.getTenantId().equals(tenantId))
            .flatMap(item -> {
                if (req.title() != null) item.setTitle(req.title());
                if (req.description() != null) item.setDescription(req.description());
                if (req.tags() != null) item.setTags(req.tags());
                if (req.thumbnailUrl() != null) item.setThumbnailUrl(req.thumbnailUrl());
                if (req.targetProducts() != null) item.setTargetProducts(req.targetProducts());
                if (req.metadata() != null) item.setMetadata(req.metadata());
                return repository.save(item);
            });
    }

    public Mono<ContentItem> updateStatus(String id, ContentItem.ContentStatus newStatus, String tenantId) {
        return repository.findById(id)
            .filter(item -> item.getTenantId().equals(tenantId))
            .flatMap(item -> {
                ContentItem.ContentStatus old = item.getStatus();
                item.setStatus(newStatus);
                if (newStatus == ContentItem.ContentStatus.PUBLISHED && old != ContentItem.ContentStatus.PUBLISHED) {
                    item.setPublishedAt(Instant.now());
                }
                return repository.save(item)
                    .doOnSuccess(saved -> publishStatusEvent(saved, old));
            });
    }

    public Mono<Void> delete(String id, String tenantId) {
        return repository.findById(id)
            .filter(item -> item.getTenantId().equals(tenantId))
            .flatMap(item -> updateStatus(id, ContentItem.ContentStatus.ARCHIVED, tenantId))
            .then();
    }

    // ── Delivery-oriented query ───────────────────────────────────────────────

    public Flux<ContentItem> findDeliverableForProduct(String tenantId, String productId, int page, int size) {
        return repository.findByTenantIdAndTargetProductsContainingAndStatus(
            tenantId, productId, ContentItem.ContentStatus.PUBLISHED, PageRequest.of(page, size));
    }

    private void publishStatusEvent(ContentItem item, ContentItem.ContentStatus previousStatus) {
        String eventType = switch (item.getStatus()) {
            case PUBLISHED -> "ContentPublished";
            case ARCHIVED  -> "ContentArchived";
            case EXPIRED   -> "ContentExpired";
            default -> null;
        };
        if (eventType == null) return;

        kafkaTemplate.send("content-events", item.getTenantId(), Map.of(
            "eventType", eventType,
            "contentId", item.getId(),
            "tenantId", item.getTenantId(),
            "type", item.getType().name(),
            "previousStatus", previousStatus.name(),
            "timestamp", Instant.now().toString()
        ));
    }
}

package dev.armanruhit.nexusvas.content.domain.repository;

import dev.armanruhit.nexusvas.content.domain.document.ContentItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ContentItemRepository extends ReactiveMongoRepository<ContentItem, String> {

    Flux<ContentItem> findByTenantIdAndStatus(String tenantId, ContentItem.ContentStatus status, Pageable pageable);

    Flux<ContentItem> findByTenantIdAndStatusAndType(
        String tenantId, ContentItem.ContentStatus status, ContentItem.ContentType type, Pageable pageable);

    Flux<ContentItem> findByTenantIdAndTargetProductsContainingAndStatus(
        String tenantId, String productId, ContentItem.ContentStatus status, Pageable pageable);

    Mono<Long> countByTenantIdAndStatus(String tenantId, ContentItem.ContentStatus status);
}

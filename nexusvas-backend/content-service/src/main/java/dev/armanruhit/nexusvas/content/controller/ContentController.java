package dev.armanruhit.nexusvas.content.controller;

import dev.armanruhit.nexusvas.content.domain.document.ContentItem;
import dev.armanruhit.nexusvas.content.dto.ContentCreateRequest;
import dev.armanruhit.nexusvas.content.dto.ContentUpdateRequest;
import dev.armanruhit.nexusvas.content.service.CdnUrlService;
import dev.armanruhit.nexusvas.content.service.ContentCatalogService;
import dev.armanruhit.nexusvas.content.service.ContentDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Content", description = "Content catalog management and delivery")
@SecurityRequirement(name = "bearerAuth")
public class ContentController {

    private final ContentCatalogService catalogService;
    private final ContentDeliveryService deliveryService;
    private final CdnUrlService cdnUrlService;

    // ── Catalog management (tenant admin) ────────────────────────────────────

    @PostMapping("/api/v1/content")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create content", description = "Creates a new content item in the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Content created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Mono<ContentItem> create(@Valid @RequestBody ContentCreateRequest req,
                                    @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        String userId   = jwt.getSubject();
        return catalogService.create(req, tenantId, userId);
    }

    @GetMapping("/api/v1/content")
    @Operation(summary = "List content", description = "Returns paginated list of content items")
    public Flux<ContentItem> list(
            @Parameter(description = "Filter by status")
            @RequestParam(defaultValue = "DRAFT") ContentItem.ContentStatus status,
            @Parameter(description = "Filter by type")
            @RequestParam(required = false) ContentItem.ContentType type,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return catalogService.list(tenantId, status, type, page, size);
    }

    @GetMapping("/api/v1/content/{id}")
    @Operation(summary = "Get content by ID", description = "Returns a single content item")
    public Mono<ContentItem> getById(
            @Parameter(description = "Content ID", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return catalogService.findById(id, tenantId)
            .switchIfEmpty(Mono.error(new ContentNotFoundException(id)));
    }

    @PatchMapping("/api/v1/content/{id}")
    @Operation(summary = "Update content", description = "Updates content metadata")
    public Mono<ContentItem> update(
            @Parameter(description = "Content ID", required = true)
            @PathVariable String id,
            @RequestBody ContentUpdateRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return catalogService.update(id, req, tenantId)
            .switchIfEmpty(Mono.error(new ContentNotFoundException(id)));
    }

    @PatchMapping("/api/v1/content/{id}/status")
    @Operation(summary = "Update content status", description = "Publish, archive, or expire content")
    public Mono<ContentItem> updateStatus(
            @Parameter(description = "Content ID", required = true)
            @PathVariable String id,
            @Parameter(description = "New status", required = true)
            @RequestParam ContentItem.ContentStatus status,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return catalogService.updateStatus(id, status, tenantId)
            .switchIfEmpty(Mono.error(new ContentNotFoundException(id)));
    }

    @DeleteMapping("/api/v1/content/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete content", description = "Archives content (soft delete)")
    public Mono<Void> delete(
            @Parameter(description = "Content ID", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return catalogService.delete(id, tenantId);
    }

    // ── Subscriber delivery ───────────────────────────────────────────────────

    @GetMapping("/api/v1/delivery/content")
    @Operation(summary = "Deliver content", description = "Returns content available to subscriber")
    public Flux<ContentItem> deliver(
            @Parameter(description = "Subscriber MSISDN", required = true)
            @RequestParam String msisdn,
            @Parameter(description = "Product ID", required = true)
            @RequestParam String productId,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return deliveryService.deliver(tenantId, msisdn, productId, page, size);
    }

    // ── Streaming endpoint (Server-Sent Events) ──────────────────────────────

    @GetMapping(value = "/api/v1/delivery/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream content updates", description = "SSE stream for real-time content delivery")
    public Flux<ServerSentEvent<ContentItem>> streamContent(
            @Parameter(description = "Subscriber MSISDN", required = true)
            @RequestParam String msisdn,
            @Parameter(description = "Product ID", required = true)
            @RequestParam String productId,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        
        return deliveryService.deliver(tenantId, msisdn, productId, 0, 100)
            .delayElements(Duration.ofMillis(100))
            .map(item -> ServerSentEvent.<ContentItem>builder()
                .id(item.getId())
                .event("content-item")
                .data(item)
                .build());
    }

    // ── CDN URL generation ───────────────────────────────────────────────────

    @PostMapping("/api/v1/cdn/signed-url")
    @Operation(summary = "Generate signed CDN URL", description = "Creates a time-limited signed URL for content access")
    public Mono<Map<String, String>> generateSignedUrl(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        String originalUrl = request.get("url");
        
        return cdnUrlService.generateSignedUrl(originalUrl, tenantId)
            .map(signedUrl -> Map.of("originalUrl", originalUrl, "signedUrl", signedUrl));
    }

    @PostMapping("/api/v1/cdn/manifest")
    @Operation(summary = "Generate CDN manifest", description = "Creates signed URLs for multiple content items")
    public Mono<Map<String, String>> generateManifest(
            @RequestBody Map<String, String> urls,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return cdnUrlService.generateManifestUrls(urls, tenantId);
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class ContentNotFoundException extends RuntimeException {
        public ContentNotFoundException(String id) { super("Content not found: " + id); }
    }
}

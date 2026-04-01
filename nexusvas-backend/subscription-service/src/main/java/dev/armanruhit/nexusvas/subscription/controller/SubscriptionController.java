package dev.armanruhit.nexusvas.subscription.controller;

import dev.armanruhit.nexusvas.subscription.domain.entity.SubscriptionProjection.SubscriptionStatus;
import dev.armanruhit.nexusvas.subscription.dto.CreateSubscriptionCommand;
import dev.armanruhit.nexusvas.subscription.dto.SubscriptionResponse;
import dev.armanruhit.nexusvas.subscription.service.SubscriptionCommandService;
import dev.armanruhit.nexusvas.subscription.service.SubscriptionProjectionService;
import dev.armanruhit.nexusvas.common_lib.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionCommandService commandService;
    private final SubscriptionProjectionService queryService;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, UUID>>> create(
            @Valid @RequestBody CreateSubscriptionCommand cmd,
            @AuthenticationPrincipal Jwt jwt) {
        // Enforce tenant isolation: override tenantId from JWT
        String tenantId = jwt.getClaimAsString("tenant_id");
        CreateSubscriptionCommand tenantCmd = new CreateSubscriptionCommand(
            tenantId, cmd.msisdn(), cmd.productId(), cmd.productName(),
            cmd.billingCycle(), cmd.priceAmount(), cmd.priceCurrency()
        );

        UUID subscriptionId = commandService.createSubscription(tenantCmd);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(Map.of("subscriptionId", subscriptionId)));
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SubscriptionResponse>>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        SubscriptionStatus statusEnum = status != null ? SubscriptionStatus.valueOf(status.toUpperCase()) : null;

        Page<SubscriptionResponse> page = queryService.list(tenantId, statusEnum, pageable)
            .map(SubscriptionResponse::from);

        return ResponseEntity.ok(ApiResponse.success(page));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getById(
            @PathVariable UUID subscriptionId,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");

        return queryService.findById(subscriptionId, tenantId)
            .map(p -> ResponseEntity.ok(ApiResponse.success(SubscriptionResponse.from(p))))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID subscriptionId,
            @RequestParam(defaultValue = "User requested cancellation") String reason,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        commandService.cancelSubscription(subscriptionId, tenantId, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Internal: check active subscription (used by Content Service) ─────────

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkActive(
            @RequestParam String msisdn,
            @RequestParam UUID productId,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        boolean active = queryService.isActiveSubscription(tenantId, msisdn, productId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("active", active)));
    }
}

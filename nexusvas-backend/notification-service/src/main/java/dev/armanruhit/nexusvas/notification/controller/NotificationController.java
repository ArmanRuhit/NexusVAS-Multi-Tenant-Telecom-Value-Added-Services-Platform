package dev.armanruhit.nexusvas.notification.controller;

import dev.armanruhit.nexusvas.notification.domain.entity.NotificationLog;
import dev.armanruhit.nexusvas.notification.domain.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification log and delivery status")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationLogRepository repository;

    @GetMapping
    @Operation(summary = "List notifications", description = "Returns paginated notification log for the tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Page<NotificationLog> list(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return repository.findByTenantIdOrderByCreatedAtDesc(
            jwt.getClaimAsString("tenant_id"), PageRequest.of(page, size));
    }

    @GetMapping("/msisdn/{msisdn}")
    @Operation(summary = "Get notifications by MSISDN", description = "Returns notification history for a specific subscriber")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Page<NotificationLog> byMsisdn(
            @Parameter(description = "Subscriber phone number (MSISDN)", required = true)
            @PathVariable String msisdn,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return repository.findByTenantIdAndMsisdnOrderByCreatedAtDesc(
            jwt.getClaimAsString("tenant_id"), msisdn, PageRequest.of(page, size));
    }
}

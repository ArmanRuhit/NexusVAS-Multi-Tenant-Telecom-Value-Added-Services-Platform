package dev.armanruhit.nexusvas.campaign.controller;

import dev.armanruhit.nexusvas.campaign.domain.entity.Campaign;
import dev.armanruhit.nexusvas.campaign.dto.CampaignCreateRequest;
import dev.armanruhit.nexusvas.campaign.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "Marketing campaign management and execution")
@SecurityRequirement(name = "bearerAuth")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create campaign", description = "Creates a new marketing campaign with targeting criteria")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Campaign created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Campaign create(@Valid @RequestBody CampaignCreateRequest req,
                           @AuthenticationPrincipal Jwt jwt) {
        return campaignService.create(req, jwt.getClaimAsString("tenant_id"), jwt.getSubject());
    }

    @GetMapping
    @Operation(summary = "List campaigns", description = "Returns paginated list of campaigns for the tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Page<Campaign> list(
            @Parameter(description = "Filter by status") 
            @RequestParam(required = false) Campaign.CampaignStatus status,
            @Parameter(description = "Page number (0-indexed)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return campaignService.list(jwt.getClaimAsString("tenant_id"), status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID", description = "Returns a single campaign with full details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign found"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public Campaign getById(
            @Parameter(description = "Campaign UUID", required = true) 
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return campaignService.findById(id, jwt.getClaimAsString("tenant_id"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update campaign status", description = "Transition campaign status (DRAFT → SCHEDULED → RUNNING → COMPLETED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public Campaign updateStatus(
            @Parameter(description = "Campaign UUID", required = true) 
            @PathVariable String id,
            @Parameter(description = "New status", required = true) 
            @RequestParam Campaign.CampaignStatus status,
            @AuthenticationPrincipal Jwt jwt) {
        return campaignService.updateStatus(id, status, jwt.getClaimAsString("tenant_id"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel campaign", description = "Cancels a campaign (sets status to CANCELLED)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Campaign cancelled"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public void cancel(
            @Parameter(description = "Campaign UUID", required = true) 
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        campaignService.delete(id, jwt.getClaimAsString("tenant_id"));
    }
}

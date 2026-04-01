package dev.armanruhit.nexusvas.ai.controller;

import dev.armanruhit.nexusvas.ai.domain.entity.ChurnScore;
import dev.armanruhit.nexusvas.ai.domain.entity.FraudAlert;
import dev.armanruhit.nexusvas.ai.service.ChurnPredictionService;
import dev.armanruhit.nexusvas.ai.service.FraudDetectionService;
import dev.armanruhit.nexusvas.ai.service.OpenRouterService;
import dev.armanruhit.nexusvas.ai.service.RagChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI-powered analytics, predictions, and chat")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final ChurnPredictionService churnPredictionService;
    private final FraudDetectionService fraudDetectionService;
    private final RagChatService ragChatService;
    private final OpenRouterService openRouterService;

    // ── Churn Prediction ─────────────────────────────────────────────────────

    @GetMapping("/churn/high-risk")
    @Operation(summary = "Get high-risk subscribers", description = "Returns subscribers with high churn risk scores")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "High-risk subscribers retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Page<ChurnScore> getHighRiskSubscribers(
            @Parameter(description = "Risk level filter")
            @RequestParam(defaultValue = "HIGH") ChurnScore.RiskLevel riskLevel,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return churnPredictionService.getHighRiskSubscribers(
            jwt.getClaimAsString("tenant_id"), riskLevel, page, size);
    }

    @GetMapping("/churn/today")
    @Operation(summary = "Get today's churn scores", description = "Returns all churn predictions for today")
    public List<ChurnScore> getTodayScores(@AuthenticationPrincipal Jwt jwt) {
        return churnPredictionService.getTodayScores(jwt.getClaimAsString("tenant_id"));
    }

    // ── Fraud Detection ───────────────────────────────────────────────────────

    @GetMapping("/fraud/alerts")
    @Operation(summary = "Get active fraud alerts", description = "Returns all active fraud alerts for the tenant")
    public List<FraudAlert> getActiveAlerts(@AuthenticationPrincipal Jwt jwt) {
        return fraudDetectionService.getActiveAlerts(jwt.getClaimAsString("tenant_id"));
    }

    @GetMapping("/fraud/alerts/{msisdn}")
    @Operation(summary = "Get fraud alerts by MSISDN", description = "Returns fraud alerts for a specific subscriber")
    public List<FraudAlert> getAlertsByMsisdn(
            @Parameter(description = "Subscriber MSISDN", required = true)
            @PathVariable String msisdn,
            @AuthenticationPrincipal Jwt jwt) {
        return fraudDetectionService.getAlertsByMsisdn(jwt.getClaimAsString("tenant_id"), msisdn);
    }

    @PostMapping("/fraud/alerts/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge fraud alert", description = "Marks a fraud alert as acknowledged")
    public ResponseEntity<Void> acknowledgeAlert(
            @Parameter(description = "Alert UUID", required = true)
            @PathVariable UUID alertId,
            @AuthenticationPrincipal Jwt jwt) {
        fraudDetectionService.acknowledgeAlert(jwt.getClaimAsString("tenant_id"), alertId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fraud/alerts/{alertId}/resolve")
    @Operation(summary = "Resolve fraud alert", description = "Marks a fraud alert as resolved with resolution notes")
    public ResponseEntity<Void> resolveAlert(
            @Parameter(description = "Alert UUID", required = true)
            @PathVariable UUID alertId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String resolution = body.getOrDefault("resolution", "Resolved by admin");
        fraudDetectionService.resolveAlert(jwt.getClaimAsString("tenant_id"), alertId, resolution);
        return ResponseEntity.ok().build();
    }

    // ── RAG Chat ──────────────────────────────────────────────────────────────

    @PostMapping("/chat")
    @Operation(summary = "AI chat with RAG", description = "Chat with AI using RAG for context retrieval")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chat response generated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Map<String, String> chat(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        String response = ragChatService.chat(
            jwt.getClaimAsString("tenant_id"),
            UUID.fromString(jwt.getSubject()),
            message
        );
        return Map.of("response", response);
    }

    // ── OpenRouter ────────────────────────────────────────────────────────────

    @PostMapping("/openrouter/chat")
    @Operation(summary = "OpenRouter chat", description = "Chat using OpenRouter API for multi-model access")
    public Map<String, String> openRouterChat(
            @RequestBody OpenRouterChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String response = openRouterService.chat(
            jwt.getClaimAsString("tenant_id"),
            UUID.fromString(jwt.getSubject()),
            request.systemPrompt(),
            request.userMessage()
        );
        return Map.of("response", response);
    }

    @GetMapping("/openrouter/models")
    @Operation(summary = "List OpenRouter models", description = "Returns available models from OpenRouter")
    public List<OpenRouterService.ModelInfo> listModels() {
        return openRouterService.listModels().block();
    }

    // Request DTO
    public record OpenRouterChatRequest(String systemPrompt, String userMessage) {}
}

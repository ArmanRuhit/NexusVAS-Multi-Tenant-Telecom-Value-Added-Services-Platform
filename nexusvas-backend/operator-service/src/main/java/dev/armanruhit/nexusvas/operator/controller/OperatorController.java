package dev.armanruhit.nexusvas.operator.controller;

import dev.armanruhit.nexusvas.operator.domain.entity.Operator;
import dev.armanruhit.nexusvas.operator.domain.entity.OperatorConfig;
import dev.armanruhit.nexusvas.operator.dto.OperatorOnboardRequest;
import dev.armanruhit.nexusvas.operator.service.OperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operators")
@RequiredArgsConstructor
@Tag(name = "Operator", description = "Multi-tenant operator management")
@SecurityRequirement(name = "bearerAuth")
public class OperatorController {

    private final OperatorService operatorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard new operator", description = "Creates a new operator tenant in the platform")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Operator onboarded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or slug already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Operator onboard(@Valid @RequestBody OperatorOnboardRequest req) {
        return operatorService.onboard(req);
    }

    @GetMapping
    @Operation(summary = "List operators", description = "Returns paginated list of operators")
    public Page<Operator> list(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) Operator.OperatorStatus status,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        return operatorService.list(status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get operator by ID", description = "Returns operator details by UUID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operator found"),
        @ApiResponse(responseCode = "404", description = "Operator not found")
    })
    public Operator getById(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id) {
        return operatorService.findById(id);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get operator by slug", description = "Returns operator details by slug")
    public Operator getBySlug(
            @Parameter(description = "Operator slug", required = true)
            @PathVariable String slug) {
        return operatorService.findBySlug(slug);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update operator status", description = "Changes operator status (ACTIVE, SUSPENDED, TERMINATED)")
    public Operator updateStatus(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New status", required = true)
            @RequestParam Operator.OperatorStatus status) {
        return operatorService.updateStatus(id, status);
    }

    // ── Config endpoints ──────────────────────────────────────────────────────

    @GetMapping("/{id}/configs")
    @Operation(summary = "Get operator configs", description = "Returns all configuration key-values for an operator")
    public List<OperatorConfig> getConfigs(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id) {
        return operatorService.getConfigs(id);
    }

    @PutMapping("/{id}/configs/{key}")
    @Operation(summary = "Set operator config", description = "Sets or updates a configuration key-value")
    public OperatorConfig setConfig(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Config key", required = true)
            @PathVariable String key,
            @RequestBody Map<String, Object> body) {
        Object value       = body.get("value");
        String description = (String) body.get("description");
        return operatorService.setConfig(id, key, value, description);
    }

    // ── Statistics endpoints ──────────────────────────────────────────────────

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get operator statistics", description = "Returns subscription and revenue statistics for an operator")
    public OperatorService.OperatorStats getStats(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id) {
        return operatorService.getStats(id);
    }

    // ── API Key management ────────────────────────────────────────────────────

    @PostMapping("/{id}/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate API key", description = "Generates a new API key for the operator")
    public Map<String, String> generateApiKey(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "default");
        String apiKey = operatorService.generateApiKey(id, name);
        return Map.of("name", name, "apiKey", apiKey);
    }

    @GetMapping("/{id}/api-keys")
    @Operation(summary = "List API keys", description = "Returns list of API key names (not the actual keys)")
    public List<Map<String, Object>> listApiKeys(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id) {
        return operatorService.listApiKeys(id);
    }

    @DeleteMapping("/{id}/api-keys/{keyId}")
    @Operation(summary = "Revoke API key", description = "Revokes an API key")
    public void revokeApiKey(
            @Parameter(description = "Operator UUID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "API Key ID", required = true)
            @PathVariable UUID keyId) {
        operatorService.revokeApiKey(id, keyId);
    }
}

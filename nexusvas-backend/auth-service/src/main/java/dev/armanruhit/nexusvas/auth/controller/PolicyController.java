package dev.armanruhit.nexusvas.auth.controller;

import dev.armanruhit.nexusvas.auth.dto.PolicyCheckRequest;
import dev.armanruhit.nexusvas.auth.dto.PolicyCheckResponse;
import dev.armanruhit.nexusvas.auth.service.RbacService;
import dev.armanruhit.nexusvas.common_lib.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final RbacService rbacService;

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<PolicyCheckResponse>> checkPermission(
            @Valid @RequestBody PolicyCheckRequest request) {
        boolean allowed = rbacService.checkPermission(request.userId(), request.resource(), request.action());
        PolicyCheckResponse response = allowed
            ? PolicyCheckResponse.granted()
            : PolicyCheckResponse.denied("User does not have permission: " + request.resource() + ":" + request.action());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/permissions/{userId}")
    public ResponseEntity<ApiResponse<Set<String>>> getPermissions(@PathVariable UUID userId) {
        Set<String> permissions = rbacService.resolvePermissions(userId);
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }
}

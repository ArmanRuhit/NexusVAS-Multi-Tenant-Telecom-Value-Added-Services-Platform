package dev.armanruhit.nexusvas.auth.controller;

import dev.armanruhit.nexusvas.auth.dto.*;
import dev.armanruhit.nexusvas.auth.service.AuthService;
import dev.armanruhit.nexusvas.common_lib.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Portal Login ──────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        LoginResponse response = authService.login(
            request,
            servletRequest.getRemoteAddr(),
            servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        LoginResponse response = authService.verifyMfa(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Token Management ──────────────────────────────────────────────────────

    @PostMapping("/token/api-key")
    public ResponseEntity<ApiResponse<TokenResponse>> authenticateApiKey(
            @RequestHeader("X-API-Key") String apiKey) {
        TokenResponse response = authService.authenticateApiKey(apiKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        TokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/token/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken,
            @AuthenticationPrincipal Jwt jwt) {
        String accessToken = authHeader != null && authHeader.startsWith("Bearer ")
            ? authHeader.substring(7) : null;

        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        String tenantId = jwt != null ? jwt.getClaimAsString("tenant_id") : null;

        authService.logout(accessToken, refreshToken, userId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Subscriber OTP ────────────────────────────────────────────────────────

    @PostMapping("/subscriber/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        authService.sendSubscriberOtp(request.msisdn(), request.tenantId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/subscriber/otp/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        TokenResponse response = authService.verifySubscriberOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── JWKS ──────────────────────────────────────────────────────────────────

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Object> jwks() {
        // Spring Authorization Server exposes this at /oauth2/jwks
        // This endpoint is a convenience redirect for service consumers
        return ResponseEntity.ok().header("Location", "/oauth2/jwks").build();
    }
}

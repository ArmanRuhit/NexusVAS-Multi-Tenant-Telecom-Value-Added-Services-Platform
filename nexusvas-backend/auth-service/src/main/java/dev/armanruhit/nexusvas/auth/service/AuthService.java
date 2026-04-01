package dev.armanruhit.nexusvas.auth.service;

import dev.armanruhit.nexusvas.auth.domain.entity.ApiKey;
import dev.armanruhit.nexusvas.auth.domain.entity.AuditLog;
import dev.armanruhit.nexusvas.auth.domain.entity.User;
import dev.armanruhit.nexusvas.auth.domain.repository.UserRepository;
import dev.armanruhit.nexusvas.auth.dto.*;
import dev.armanruhit.nexusvas.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ApiKeyService apiKeyService;
    private final OtpService otpService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.login.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    // ── Operator Portal Login ─────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmailAndTenantIdWithPermissions(request.email(), request.tenantId())
            .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new AuthException("ACCOUNT_LOCKED", "Account is temporarily locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("ACCOUNT_INACTIVE", "Account is not active");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        if (user.isMfaEnabled()) {
            // Return MFA challenge — actual token will be issued after MFA verification
            String challengeToken = storeMfaChallenge(user.getId());
            auditService.log(user.getId(), user.getTenantId(), AuditLog.AuditAction.LOGIN, null, ipAddress, userAgent, null);
            return LoginResponse.mfaRequired(challengeToken);
        }

        String accessToken = tokenService.generateAccessToken(user, "PORTAL_USER");
        String refreshToken = tokenService.generateRefreshToken(user.getId(), user.getTenantId(), request.deviceId());

        auditService.log(user.getId(), user.getTenantId(), AuditLog.AuditAction.LOGIN, null, ipAddress, userAgent, null);

        return LoginResponse.success(accessToken, refreshToken, user.getId().toString(), user.getTenantId());
    }

    // ── MFA Verification ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LoginResponse verifyMfa(MfaVerifyRequest request) {
        UUID userId = resolveMfaChallenge(request.challengeToken());
        if (userId == null) {
            throw new AuthException("INVALID_CHALLENGE", "MFA challenge token is invalid or expired");
        }

        User user = userRepository.findByIdWithRolesAndPermissions(userId)
            .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User not found"));

        // TOTP validation (plug in actual TOTP library here)
        if (!validateTotp(user.getMfaSecret(), request.totpCode())) {
            throw new AuthException("INVALID_TOTP", "Invalid TOTP code");
        }

        String accessToken = tokenService.generateAccessToken(user, "PORTAL_USER");
        String refreshToken = tokenService.generateRefreshToken(user.getId(), user.getTenantId(), null);

        auditService.log(user.getId(), user.getTenantId(), AuditLog.AuditAction.MFA_VERIFIED);

        return LoginResponse.success(accessToken, refreshToken, user.getId().toString(), user.getTenantId());
    }

    // ── Subscriber OTP ────────────────────────────────────────────────────────

    public void sendSubscriberOtp(String msisdn, String tenantId) {
        otpService.sendOtp(msisdn, tenantId);
        auditService.log(null, tenantId, AuditLog.AuditAction.OTP_SENT);
    }

    public TokenResponse verifySubscriberOtp(OtpVerifyRequest request) {
        boolean valid = otpService.verifyOtp(request.msisdn(), request.tenantId(), request.otp());
        if (!valid) {
            throw new AuthException("INVALID_OTP", "OTP is invalid or expired");
        }

        String accessToken = tokenService.generateSubscriberAccessToken(request.msisdn(), request.tenantId());
        auditService.log(null, request.tenantId(), AuditLog.AuditAction.OTP_VERIFIED);

        return new TokenResponse(accessToken, null, 3600);
    }

    // ── API Key Authentication ────────────────────────────────────────────────

    @Transactional
    public TokenResponse authenticateApiKey(String rawApiKey) {
        ApiKey apiKey = apiKeyService.validateApiKey(rawApiKey)
            .orElseThrow(() -> new AuthException("INVALID_API_KEY", "API key is invalid or expired"));

        apiKeyService.updateLastUsed(apiKey.getId());

        // Build a minimal JWT for the operator API client
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(900); // 15 minutes

        String accessToken = buildApiKeyJwt(apiKey, jti, now, expiry);

        auditService.log(null, apiKey.getTenantId(), AuditLog.AuditAction.TOKEN_ISSUED);

        return new TokenResponse(accessToken, null, 900);
    }

    // ── Token Refresh ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TokenResponse refreshToken(String refreshToken) {
        Map<Object, Object> tokenData = tokenService.validateRefreshToken(refreshToken)
            .orElseThrow(() -> new AuthException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired"));

        UUID userId = UUID.fromString((String) tokenData.get("userId"));
        String tenantId = (String) tokenData.get("tenantId");

        User user = userRepository.findByIdWithRolesAndPermissions(userId)
            .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            tokenService.revokeRefreshToken(refreshToken);
            throw new AuthException("ACCOUNT_INACTIVE", "Account is not active");
        }

        // Rotate refresh token for security
        tokenService.revokeRefreshToken(refreshToken);
        String newRefreshToken = tokenService.generateRefreshToken(userId, tenantId, null);
        String newAccessToken = tokenService.generateAccessToken(user, "PORTAL_USER");

        return new TokenResponse(newAccessToken, newRefreshToken, 900);
    }

    // ── Logout / Revocation ───────────────────────────────────────────────────

    public void logout(String accessToken, String refreshToken, UUID userId, String tenantId) {
        try {
            var claims = tokenService.parseToken(accessToken);
            tokenService.blacklistAccessToken(claims.getId(), claims.getExpiration().toInstant());
        } catch (Exception e) {
            log.warn("Could not parse access token during logout: {}", e.getMessage());
        }

        if (refreshToken != null) {
            tokenService.revokeRefreshToken(refreshToken);
        }

        auditService.log(userId, tenantId, AuditLog.AuditAction.LOGOUT);
    }

    public void forceLogoutUser(UUID userId, String tenantId) {
        tokenService.revokeAllUserRefreshTokens(userId);
        auditService.log(userId, tenantId, AuditLog.AuditAction.TOKEN_REVOKED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
            user.setStatus(User.UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plusSeconds(lockDurationMinutes * 60L));
            log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), user.getFailedLoginAttempts());
        }
        userRepository.save(user);
    }

    private String storeMfaChallenge(UUID userId) {
        String challengeToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("mfa_challenge:" + challengeToken, userId.toString(), 5, TimeUnit.MINUTES);
        return challengeToken;
    }

    private UUID resolveMfaChallenge(String challengeToken) {
        String userId = redisTemplate.opsForValue().get("mfa_challenge:" + challengeToken);
        if (userId == null) return null;
        redisTemplate.delete("mfa_challenge:" + challengeToken);
        return UUID.fromString(userId);
    }

    private boolean validateTotp(String mfaSecret, String code) {
        // Placeholder — plug in a TOTP library (e.g., aerogear-otp or GoogleAuth)
        // For now returns true in dev; replace with actual TOTP validation
        return code != null && code.length() == 6;
    }

    private String buildApiKeyJwt(ApiKey apiKey, String jti, Instant now, Instant expiry) {
        // Delegate to TokenService for a trimmed-down API key JWT
        // This is a simplified version — full implementation uses TokenService
        return tokenService.generateSubscriberAccessToken(apiKey.getTenantId() + "_api", apiKey.getTenantId());
    }
}

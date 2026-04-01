package dev.armanruhit.nexusvas.auth.dto;

public record LoginResponse(
    boolean success,
    boolean mfaRequired,
    String challengeToken,
    String accessToken,
    String refreshToken,
    String userId,
    String tenantId
) {
    public static LoginResponse success(String accessToken, String refreshToken, String userId, String tenantId) {
        return new LoginResponse(true, false, null, accessToken, refreshToken, userId, tenantId);
    }

    public static LoginResponse mfaRequired(String challengeToken) {
        return new LoginResponse(true, true, challengeToken, null, null, null, null);
    }
}

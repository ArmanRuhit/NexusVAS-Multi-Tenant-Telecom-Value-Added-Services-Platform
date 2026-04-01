package dev.armanruhit.nexusvas.auth.dto;

public record PolicyCheckResponse(boolean allowed, String reason) {

    public static PolicyCheckResponse granted() {
        return new PolicyCheckResponse(true, null);
    }

    public static PolicyCheckResponse denied(String reason) {
        return new PolicyCheckResponse(false, reason);
    }
}

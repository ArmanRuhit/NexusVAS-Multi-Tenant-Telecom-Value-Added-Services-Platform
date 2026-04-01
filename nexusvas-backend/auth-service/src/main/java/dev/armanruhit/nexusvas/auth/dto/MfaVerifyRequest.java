package dev.armanruhit.nexusvas.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
    @NotBlank String challengeToken,
    @NotBlank @Pattern(regexp = "\\d{6}") String totpCode
) {}

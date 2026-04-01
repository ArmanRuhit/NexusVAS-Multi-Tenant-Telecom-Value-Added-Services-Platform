package dev.armanruhit.nexusvas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String tenantId,
    String deviceId
) {}

package dev.armanruhit.nexusvas.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PolicyCheckRequest(
    @NotNull UUID userId,
    @NotBlank String tenantId,
    @NotBlank String resource,
    @NotBlank String action
) {}

package dev.armanruhit.nexusvas.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpSendRequest(
    @NotBlank String msisdn,
    @NotBlank String tenantId
) {}

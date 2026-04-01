package dev.armanruhit.nexusvas.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
    @NotBlank String msisdn,
    @NotBlank String tenantId,
    @NotBlank @Pattern(regexp = "\\d{6}") String otp
) {}

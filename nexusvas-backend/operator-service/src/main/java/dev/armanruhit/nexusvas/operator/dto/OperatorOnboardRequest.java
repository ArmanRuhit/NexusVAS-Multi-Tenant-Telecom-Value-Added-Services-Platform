package dev.armanruhit.nexusvas.operator.dto;

import dev.armanruhit.nexusvas.operator.domain.entity.Operator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

public record OperatorOnboardRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must be lowercase alphanumeric with hyphens")
    @Size(max = 50) String slug,
    @NotBlank @Size(min = 2, max = 2) String country,
    String timezone,
    String currency,
    @NotNull Operator.BillingModel billingModel,
    @NotBlank @Email String contactEmail,
    String contactPhone,
    Map<String, Object> address,
    LocalDate contractStartDate,
    LocalDate contractEndDate
) {}

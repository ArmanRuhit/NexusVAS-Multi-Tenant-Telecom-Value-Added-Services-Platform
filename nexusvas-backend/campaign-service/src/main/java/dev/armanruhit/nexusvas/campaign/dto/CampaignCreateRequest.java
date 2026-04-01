package dev.armanruhit.nexusvas.campaign.dto;

import dev.armanruhit.nexusvas.campaign.domain.entity.Campaign;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record CampaignCreateRequest(
    @NotBlank String name,
    String description,
    @NotNull Campaign.CampaignType type,
    @NotNull Map<String, Object> targetCriteria,
    @NotNull Map<String, Object> contentTemplate,
    Instant startAt,
    Instant endAt
) {}

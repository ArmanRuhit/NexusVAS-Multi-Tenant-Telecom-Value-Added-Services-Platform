package dev.armanruhit.nexusvas.content.dto;

import dev.armanruhit.nexusvas.content.domain.document.ContentItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ContentCreateRequest(
    @NotNull ContentItem.ContentType type,
    @NotBlank String title,
    String description,
    List<String> tags,
    String language,
    ContentItem.ContentVisibility visibility,
    List<String> targetProducts,
    String thumbnailUrl,
    Instant expiresAt,
    Map<String, Object> metadata
) {}

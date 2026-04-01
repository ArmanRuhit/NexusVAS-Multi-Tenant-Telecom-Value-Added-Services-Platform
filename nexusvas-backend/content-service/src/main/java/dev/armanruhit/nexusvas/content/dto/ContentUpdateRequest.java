package dev.armanruhit.nexusvas.content.dto;

import java.util.List;
import java.util.Map;

public record ContentUpdateRequest(
    String title,
    String description,
    List<String> tags,
    String thumbnailUrl,
    List<String> targetProducts,
    Map<String, Object> metadata
) {}

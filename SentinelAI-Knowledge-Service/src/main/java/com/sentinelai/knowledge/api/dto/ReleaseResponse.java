package com.sentinelai.knowledge.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReleaseResponse(
        UUID id,
        UUID repositoryId,
        String repositoryName,
        String version,
        String tagName,
        Instant publishedAt
) {
}

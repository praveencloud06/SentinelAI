package com.sentinelai.knowledge.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommitResponse(
        String hash,
        UUID repositoryId,
        String repositoryName,
        String authorName,
        String authorEmail,
        String message,
        Instant committedAt,
        List<String> changedFiles
) {
}

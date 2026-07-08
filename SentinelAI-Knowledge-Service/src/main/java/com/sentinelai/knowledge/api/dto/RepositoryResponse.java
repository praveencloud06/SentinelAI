package com.sentinelai.knowledge.api.dto;

import com.sentinelai.knowledge.domain.SourceSystem;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        String tenantId,
        SourceSystem sourceSystem,
        String externalId,
        String name,
        String defaultBranch,
        String url,
        Instant lastSyncedAt
) {
}

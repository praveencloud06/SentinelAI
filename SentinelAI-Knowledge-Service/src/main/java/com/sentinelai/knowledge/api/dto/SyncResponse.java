package com.sentinelai.knowledge.api.dto;

import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.domain.SyncStatus;

import java.time.Instant;
import java.util.UUID;

public record SyncResponse(
        UUID syncId,
        SourceSystem sourceSystem,
        SyncStatus status,
        int eventsCreated,
        Instant startedAt,
        Instant completedAt,
        String message
) {
}

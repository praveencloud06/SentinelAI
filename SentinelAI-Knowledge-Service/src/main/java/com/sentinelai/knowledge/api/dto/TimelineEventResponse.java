package com.sentinelai.knowledge.api.dto;

import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.SourceSystem;

import java.time.Instant;
import java.util.UUID;

public record TimelineEventResponse(
        UUID id,
        SourceSystem sourceSystem,
        EngineeringEventType eventType,
        String subjectType,
        String subjectId,
        String title,
        Instant occurredAt,
        String metadataJson
) {
}

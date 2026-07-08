package com.sentinelai.knowledge.api.dto;

import com.sentinelai.knowledge.domain.RelationshipType;

import java.time.Instant;
import java.util.UUID;

public record RelationshipResponse(
        UUID id,
        String sourceType,
        String sourceId,
        RelationshipType relationshipType,
        String targetType,
        String targetId,
        Instant createdAt
) {
}

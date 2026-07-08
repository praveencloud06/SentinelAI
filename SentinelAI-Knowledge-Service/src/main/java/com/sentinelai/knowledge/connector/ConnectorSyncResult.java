package com.sentinelai.knowledge.connector;

import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;

import java.util.List;

public record ConnectorSyncResult(
        String message,
        List<EngineeringEventEntity> events
) {
}

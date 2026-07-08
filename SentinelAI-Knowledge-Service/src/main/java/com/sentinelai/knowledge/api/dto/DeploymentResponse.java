package com.sentinelai.knowledge.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        UUID repositoryId,
        String repositoryName,
        String releaseVersion,
        String environment,
        String buildNumber,
        String buildStatus,
        Long buildDurationMs,
        String commitRange,
        String jiraVersions,
        Instant deployedAt
) {
}

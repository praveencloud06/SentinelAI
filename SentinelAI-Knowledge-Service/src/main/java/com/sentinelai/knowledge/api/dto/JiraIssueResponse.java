package com.sentinelai.knowledge.api.dto;

import java.time.Instant;

public record JiraIssueResponse(
        String issueKey,
        String projectKey,
        String summary,
        String issueType,
        String status,
        String assignee,
        String fixVersion,
        String labels,
        String components,
        Instant updatedAt
) {
}

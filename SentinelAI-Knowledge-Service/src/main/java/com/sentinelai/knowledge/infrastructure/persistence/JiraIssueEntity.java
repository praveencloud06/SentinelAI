package com.sentinelai.knowledge.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jira_issues")
public class JiraIssueEntity {
    @Id
    @Column(name = "issue_key", length = 80)
    private String issueKey;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId = "default";

    @Column(name = "project_key", nullable = false, length = 80)
    private String projectKey;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(name = "issue_type", length = 80)
    private String issueType;

    @Column(length = 120)
    private String status;

    @Column(length = 200)
    private String assignee;

    @Column(name = "fix_version", length = 160)
    private String fixVersion;

    @Column(length = 512)
    private String labels;

    @Column(length = 512)
    private String components;

    @Column(name = "updated_at")
    private Instant updatedAt;
}

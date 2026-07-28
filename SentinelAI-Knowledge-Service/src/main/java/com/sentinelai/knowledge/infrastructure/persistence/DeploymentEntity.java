package com.sentinelai.knowledge.infrastructure.persistence;

import com.sentinelai.knowledge.domain.EmbeddingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deployments")
public class DeploymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    @JsonIgnore
    private RepositoryEntity repository;

    @Column(name = "release_version", length = 160)
    private String releaseVersion;

    @Column(nullable = false, length = 120)
    private String environment;

    @Column(name = "build_number", length = 120)
    private String buildNumber;

    @Column(name = "build_status", length = 80)
    private String buildStatus;

    @Column(name = "build_duration_ms")
    private Long buildDurationMs;

    @Column(name = "commit_range", length = 512)
    private String commitRange;

    @Column(name = "jira_versions", length = 512)
    private String jiraVersions;

    @Column(name = "deployed_at", nullable = false)
    private Instant deployedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", length = 40)
    private EmbeddingStatus embeddingStatus;
}
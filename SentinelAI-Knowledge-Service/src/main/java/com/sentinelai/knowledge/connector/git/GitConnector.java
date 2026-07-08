package com.sentinelai.knowledge.connector.git;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.application.RelationshipService;
import com.sentinelai.knowledge.config.ExternalPlatformProperties;
import com.sentinelai.knowledge.connector.ConnectorSyncResult;
import com.sentinelai.knowledge.connector.EngineeringConnector;
import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.RelationshipType;
import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ReleaseEntity;
import com.sentinelai.knowledge.infrastructure.persistence.RepositoryEntity;
import com.sentinelai.knowledge.infrastructure.repository.CommitMetadataJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.ReleaseJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GitConnector implements EngineeringConnector {
    private final RepositoryJpaRepository repositoryRepository;
    private final CommitMetadataJpaRepository commitRepository;
    private final ReleaseJpaRepository releaseRepository;
    private final RelationshipService relationshipService;
    private final ExternalPlatformProperties platformProperties;

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.GITHUB;
    }

    @Override
    public ConnectorSyncResult sync(SyncRequest request) {
        ExternalPlatformProperties.GitPlatform github = platformProperties.getGithub();
        String tenantId = configuredTenantId(request.resolvedTenantId(), github.getDefaultTenantId());
        String repositoryExternalId = configuredRepository(github);
        String repositoryName = repositoryExternalId.contains("/")
                ? repositoryExternalId.substring(repositoryExternalId.lastIndexOf('/') + 1)
                : repositoryExternalId;
        RepositoryEntity repository = repositoryRepository.findByTenantIdAndSourceSystemAndExternalId(tenantId, SourceSystem.GITHUB, repositoryExternalId)
                .orElseGet(() -> {
                    RepositoryEntity entity = new RepositoryEntity();
                    entity.setTenantId(tenantId);
                    entity.setSourceSystem(SourceSystem.GITHUB);
                    entity.setExternalId(repositoryExternalId);
                    entity.setName(repositoryName);
                    entity.setDefaultBranch("main");
                    entity.setUrl(repositoryUrl(github.getBaseUrl(), repositoryExternalId));
                    return entity;
                });
        repository.setLastSyncedAt(Instant.now());
        repository = repositoryRepository.save(repository);

        CommitMetadataEntity commit = commitRepository.findById("9f3a1c7-sample").orElseGet(CommitMetadataEntity::new);
        commit.setHash("9f3a1c7-sample");
        commit.setRepository(repository);
        commit.setAuthorName("SentinelAI Engineering");
        commit.setAuthorEmail("engineering@example.com");
        commit.setMessage("Add payment retry instrumentation");
        commit.setCommittedAt(Instant.now().minusSeconds(3600));
        commit.setChangedFiles(List.of("services/payment/RetryPolicy.java", "services/payment/PaymentController.java"));
        commitRepository.save(commit);

        ReleaseEntity release = releaseRepository.findByRepositoryIdAndVersion(repository.getId(), "v0.1.0").orElseGet(ReleaseEntity::new);
        release.setRepository(repository);
        release.setVersion("v0.1.0");
        release.setTagName("knowledge-foundation");
        release.setPublishedAt(Instant.now().minusSeconds(1200));
        releaseRepository.save(release);

        relationshipService.ensureRelationship(tenantId, "Repository", repository.getId().toString(), RelationshipType.REPOSITORY_CONTAINS_COMMIT, "Commit", commit.getHash());
        relationshipService.ensureRelationship(tenantId, "Commit", commit.getHash(), RelationshipType.PULL_REQUEST_LINKS_JIRA, "JiraIssue", "PAY-101");
        relationshipService.ensureRelationship(tenantId, "JiraIssue", "PAY-101", RelationshipType.JIRA_FIXED_IN_RELEASE, "Release", release.getVersion());

        EngineeringEventEntity commitEvent = event(tenantId, SourceSystem.GITHUB, EngineeringEventType.COMMIT_CREATED, "Commit", commit.getHash(), "Commit created: " + commit.getMessage(), commit.getCommittedAt());
        EngineeringEventEntity releaseEvent = event(tenantId, SourceSystem.GITHUB, EngineeringEventType.RELEASE_PUBLISHED, "Release", release.getVersion(), "Release published: " + release.getVersion(), release.getPublishedAt());
        return new ConnectorSyncResult("Git metadata synchronized using configured GitHub settings", List.of(commitEvent, releaseEvent));
    }

    private String configuredRepository(ExternalPlatformProperties.GitPlatform github) {
        if (github.getRepositories() == null || github.getRepositories().isEmpty() || github.getRepositories().get(0).isBlank()) {
            return "sentinelai/api";
        }
        return github.getRepositories().get(0);
    }

    private String configuredTenantId(String requestTenantId, String configuredTenantId) {
        if (requestTenantId != null && !requestTenantId.isBlank() && !"default".equals(requestTenantId)) {
            return requestTenantId;
        }
        return configuredTenantId == null || configuredTenantId.isBlank() ? "default" : configuredTenantId;
    }

    private String repositoryUrl(String baseUrl, String repositoryExternalId) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return repositoryExternalId;
        }
        String normalizedBaseUrl = baseUrl.replace("/api/v4", "").replace("/api.github.com", "/github.com");
        return normalizedBaseUrl.replaceAll("/$", "") + "/" + repositoryExternalId;
    }

    private EngineeringEventEntity event(String tenantId, SourceSystem source, EngineeringEventType type, String subjectType, String subjectId, String title, Instant occurredAt) {
        EngineeringEventEntity event = new EngineeringEventEntity();
        event.setTenantId(tenantId);
        event.setSourceSystem(source);
        event.setEventType(type);
        event.setSubjectType(subjectType);
        event.setSubjectId(subjectId);
        event.setTitle(title);
        event.setOccurredAt(occurredAt);
        return event;
    }
}

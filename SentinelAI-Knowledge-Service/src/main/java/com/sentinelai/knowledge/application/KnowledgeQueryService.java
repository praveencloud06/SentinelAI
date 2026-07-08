package com.sentinelai.knowledge.application;

import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ReleaseEntity;
import com.sentinelai.knowledge.infrastructure.persistence.RepositoryEntity;
import com.sentinelai.knowledge.infrastructure.repository.CommitMetadataJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.DeploymentJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.EngineeringEventJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.JiraIssueJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.ReleaseJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService {
    private final RepositoryJpaRepository repositoryRepository;
    private final CommitMetadataJpaRepository commitRepository;
    private final JiraIssueJpaRepository jiraIssueRepository;
    private final DeploymentJpaRepository deploymentRepository;
    private final ReleaseJpaRepository releaseRepository;
    private final EngineeringEventJpaRepository eventRepository;

    @Transactional(readOnly = true)
    public List<RepositoryEntity> repositories() {
        return repositoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RepositoryEntity repository(UUID id) {
        return repositoryRepository.findById(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<CommitMetadataEntity> commits(UUID repositoryId) {
        return repositoryId == null ? commitRepository.findAll() : commitRepository.findByRepositoryId(repositoryId);
    }

    @Transactional(readOnly = true)
    public CommitMetadataEntity commit(String hash) {
        return commitRepository.findById(hash).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<JiraIssueEntity> jiraIssues(String projectKey, String fixVersion) {
        if (projectKey != null) {
            return jiraIssueRepository.findByProjectKey(projectKey);
        }
        if (fixVersion != null) {
            return jiraIssueRepository.findByFixVersion(fixVersion);
        }
        return jiraIssueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DeploymentEntity> deployments(String releaseVersion, String commitHash) {
        if (releaseVersion != null) {
            return deploymentRepository.findByReleaseVersion(releaseVersion);
        }
        if (commitHash != null) {
            return deploymentRepository.findByCommitRangeContainingIgnoreCase(commitHash);
        }
        return deploymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ReleaseEntity> releases(String version) {
        return version == null ? releaseRepository.findAll() : releaseRepository.findByVersion(version);
    }

    @Transactional(readOnly = true)
    public List<EngineeringEventEntity> timeline(String tenantId, int limit) {
        return eventRepository.findByTenantIdOrderByOccurredAtDesc(tenantId == null ? "default" : tenantId, PageRequest.of(0, Math.min(limit, 200)));
    }
}

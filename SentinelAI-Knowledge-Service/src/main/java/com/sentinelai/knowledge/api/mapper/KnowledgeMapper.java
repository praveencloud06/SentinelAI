package com.sentinelai.knowledge.api.mapper;

import com.sentinelai.knowledge.api.dto.CommitResponse;
import com.sentinelai.knowledge.api.dto.DeploymentResponse;
import com.sentinelai.knowledge.api.dto.JiraIssueResponse;
import com.sentinelai.knowledge.api.dto.RelationshipResponse;
import com.sentinelai.knowledge.api.dto.ReleaseResponse;
import com.sentinelai.knowledge.api.dto.RepositoryResponse;
import com.sentinelai.knowledge.api.dto.TimelineEventResponse;
import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringRelationshipEntity;
import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ReleaseEntity;
import com.sentinelai.knowledge.infrastructure.persistence.RepositoryEntity;
import org.hibernate.Hibernate;

public final class KnowledgeMapper {
    private KnowledgeMapper() {
    }

    public static RepositoryResponse toResponse(RepositoryEntity entity) {
        return new RepositoryResponse(entity.getId(), entity.getTenantId(), entity.getSourceSystem(), entity.getExternalId(),
                entity.getName(), entity.getDefaultBranch(), entity.getUrl(), entity.getLastSyncedAt());
    }

    public static CommitResponse toResponse(CommitMetadataEntity entity) {
        java.util.UUID repoId = null;
        String repoName = null;
        if (entity.getRepository() != null && Hibernate.isInitialized(entity.getRepository())) {
            repoId = entity.getRepository().getId();
            repoName = entity.getRepository().getName();
        }
        return new CommitResponse(entity.getHash(), repoId, repoName,
                entity.getAuthorName(), entity.getAuthorEmail(), entity.getMessage(), entity.getCommittedAt(), entity.getChangedFiles());
    }

    public static JiraIssueResponse toResponse(JiraIssueEntity entity) {
        return new JiraIssueResponse(entity.getIssueKey(), entity.getProjectKey(), entity.getSummary(), entity.getIssueType(),
                entity.getStatus(), entity.getAssignee(), entity.getFixVersion(), entity.getLabels(), entity.getComponents(), entity.getUpdatedAt());
    }

    public static ReleaseResponse toResponse(ReleaseEntity entity) {
        java.util.UUID repoId = null;
        String repoName = null;
        if (entity.getRepository() != null && Hibernate.isInitialized(entity.getRepository())) {
            repoId = entity.getRepository().getId();
            repoName = entity.getRepository().getName();
        }
        return new ReleaseResponse(entity.getId(), repoId, repoName,
                entity.getVersion(), entity.getTagName(), entity.getPublishedAt());
    }

    public static DeploymentResponse toResponse(DeploymentEntity entity) {
        java.util.UUID repoId = null;
        String repoName = null;
        if (entity.getRepository() != null && Hibernate.isInitialized(entity.getRepository())) {
            repoId = entity.getRepository().getId();
            repoName = entity.getRepository().getName();
        }
        return new DeploymentResponse(entity.getId(), repoId, repoName, entity.getReleaseVersion(), entity.getEnvironment(),
                entity.getBuildNumber(), entity.getBuildStatus(), entity.getBuildDurationMs(), entity.getCommitRange(), entity.getJiraVersions(), entity.getDeployedAt());
    }

    public static TimelineEventResponse toResponse(EngineeringEventEntity entity) {
        return new TimelineEventResponse(entity.getId(), entity.getSourceSystem(), entity.getEventType(), entity.getSubjectType(),
                entity.getSubjectId(), entity.getTitle(), entity.getOccurredAt(), entity.getMetadataJson());
    }

    public static RelationshipResponse toResponse(EngineeringRelationshipEntity entity) {
        return new RelationshipResponse(entity.getId(), entity.getSourceType(), entity.getSourceId(), entity.getRelationshipType(),
                entity.getTargetType(), entity.getTargetId(), entity.getCreatedAt());
    }
}
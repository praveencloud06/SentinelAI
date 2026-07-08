package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JiraIssueJpaRepository extends JpaRepository<JiraIssueEntity, String> {
    List<JiraIssueEntity> findByProjectKey(String projectKey);
    List<JiraIssueEntity> findByFixVersion(String fixVersion);
}

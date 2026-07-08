package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentJpaRepository extends JpaRepository<DeploymentEntity, UUID> {
    List<DeploymentEntity> findByReleaseVersion(String releaseVersion);
    List<DeploymentEntity> findByCommitRangeContainingIgnoreCase(String commitHash);
    Optional<DeploymentEntity> findByBuildNumberAndEnvironment(String buildNumber, String environment);
}

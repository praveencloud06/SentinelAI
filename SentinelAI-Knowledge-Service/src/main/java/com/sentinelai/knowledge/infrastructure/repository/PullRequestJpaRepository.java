package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.PullRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PullRequestJpaRepository extends JpaRepository<PullRequestEntity, UUID> {
    Optional<PullRequestEntity> findByRepositoryIdAndNumber(UUID repositoryId, Integer number);
}

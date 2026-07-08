package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommitMetadataJpaRepository extends JpaRepository<CommitMetadataEntity, String> {
    List<CommitMetadataEntity> findByRepositoryId(UUID repositoryId);
}

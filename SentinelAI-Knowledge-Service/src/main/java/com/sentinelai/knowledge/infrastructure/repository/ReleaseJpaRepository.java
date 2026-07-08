package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.ReleaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReleaseJpaRepository extends JpaRepository<ReleaseEntity, UUID> {
    List<ReleaseEntity> findByVersion(String version);
    Optional<ReleaseEntity> findByRepositoryIdAndVersion(UUID repositoryId, String version);
}

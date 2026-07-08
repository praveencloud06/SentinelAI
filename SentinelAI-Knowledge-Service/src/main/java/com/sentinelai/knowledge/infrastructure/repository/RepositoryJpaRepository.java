package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.infrastructure.persistence.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositoryJpaRepository extends JpaRepository<RepositoryEntity, UUID> {
    Optional<RepositoryEntity> findByTenantIdAndSourceSystemAndExternalId(String tenantId, SourceSystem sourceSystem, String externalId);
}

package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EngineeringEventJpaRepository extends JpaRepository<EngineeringEventEntity, UUID> {
    List<EngineeringEventEntity> findByTenantIdOrderByOccurredAtDesc(String tenantId, Pageable pageable);
}

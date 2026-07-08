package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.domain.RelationshipType;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EngineeringRelationshipJpaRepository extends JpaRepository<EngineeringRelationshipEntity, UUID> {
    List<EngineeringRelationshipEntity> findBySourceTypeAndSourceId(String sourceType, String sourceId);
    List<EngineeringRelationshipEntity> findByTargetTypeAndTargetId(String targetType, String targetId);

    Optional<EngineeringRelationshipEntity> findByTenantIdAndSourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
            String tenantId,
            String sourceType,
            String sourceId,
            RelationshipType relationshipType,
            String targetType,
            String targetId
    );
}

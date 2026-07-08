package com.sentinelai.knowledge.application;

import com.sentinelai.knowledge.domain.RelationshipType;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringRelationshipEntity;
import com.sentinelai.knowledge.infrastructure.repository.EngineeringRelationshipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RelationshipService {
    private final EngineeringRelationshipJpaRepository relationshipRepository;

    @Transactional
    public EngineeringRelationshipEntity ensureRelationship(String tenantId, String sourceType, String sourceId,
                                                            RelationshipType type, String targetType, String targetId) {
        return relationshipRepository.findByTenantIdAndSourceTypeAndSourceIdAndRelationshipTypeAndTargetTypeAndTargetId(
                tenantId, sourceType, sourceId, type, targetType, targetId
        ).orElseGet(() -> {
            EngineeringRelationshipEntity relationship = new EngineeringRelationshipEntity();
            relationship.setTenantId(tenantId);
            relationship.setSourceType(sourceType);
            relationship.setSourceId(sourceId);
            relationship.setRelationshipType(type);
            relationship.setTargetType(targetType);
            relationship.setTargetId(targetId);
            return relationshipRepository.save(relationship);
        });
    }

    @Transactional(readOnly = true)
    public List<EngineeringRelationshipEntity> find(String sourceType, String sourceId, String targetType, String targetId) {
        if (sourceType != null && sourceId != null) {
            return relationshipRepository.findBySourceTypeAndSourceId(sourceType, sourceId);
        }
        if (targetType != null && targetId != null) {
            return relationshipRepository.findByTargetTypeAndTargetId(targetType, targetId);
        }
        return relationshipRepository.findAll();
    }
}

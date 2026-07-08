package com.sentinelai.knowledge.infrastructure.persistence;

import com.sentinelai.knowledge.domain.RelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "engineering_relationships", uniqueConstraints = @UniqueConstraint(name = "uk_relationship_edge", columnNames = {"tenant_id", "source_type", "source_id", "relationship_type", "target_type", "target_id"}))
public class EngineeringRelationshipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId = "default";

    @Column(name = "source_type", nullable = false, length = 120)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 256)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 120)
    private RelationshipType relationshipType;

    @Column(name = "target_type", nullable = false, length = 120)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 256)
    private String targetId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}

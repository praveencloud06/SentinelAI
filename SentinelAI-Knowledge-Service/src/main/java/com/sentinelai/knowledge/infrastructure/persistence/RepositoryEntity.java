package com.sentinelai.knowledge.infrastructure.persistence;

import com.sentinelai.knowledge.domain.SourceSystem;
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
@Table(name = "repositories", uniqueConstraints = @UniqueConstraint(name = "uk_repository_external_id", columnNames = {"tenant_id", "source_system", "external_id"}))
public class RepositoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId = "default";

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 40)
    private SourceSystem sourceSystem;

    @Column(name = "external_id", nullable = false, length = 256)
    private String externalId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(name = "default_branch", length = 120)
    private String defaultBranch;

    @Column(length = 512)
    private String url;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

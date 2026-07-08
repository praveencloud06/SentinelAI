package com.sentinelai.knowledge.infrastructure.persistence;

import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.SourceSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "engineering_events")
public class EngineeringEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId = "default";

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system", nullable = false, length = 40)
    private SourceSystem sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private EngineeringEventType eventType;

    @Column(name = "subject_type", nullable = false, length = 120)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, length = 256)
    private String subjectId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "text")
    private String metadataJson;
}

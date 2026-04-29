package com.sentinel.ai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "knowledge_base")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBaseEntry {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String logText;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "real[]")
    private float[] embedding;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;
}

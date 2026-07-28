package com.sentinelai.knowledge.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pgvector.PGvector;
import com.sentinelai.knowledge.domain.EmbeddingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for storing vector embeddings of engineering content.
 * Uses pgvector for similarity search.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "knowledge_embeddings", 
       uniqueConstraints = @UniqueConstraint(name = "uk_embedding_entity", 
                                               columnNames = {"entity_type", "entity_id", "chunk_number"}))
public class KnowledgeEmbeddingEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;
    
    @Column(name = "entity_id", nullable = false, length = 256)
    private String entityId;
    
    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber = 0;
    
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    
    /**
     * Vector embedding stored as pgvector type.
     * Dimension depends on the embedding model (e.g., 1536 for text-embedding-3-small).
     */
    @Column(columnDefinition = "vector(1536)")
    private PGvector embedding;
    
    @Column(name = "embedding_model", nullable = false, length = 120)
    private String embeddingModel;
    
    @Column(name = "embedding_version", nullable = false, length = 40)
    private String embeddingVersion = "v1";
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmbeddingStatus status = EmbeddingStatus.PENDING;
    
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    
    @Column(name = "last_embedded")
    private Instant lastEmbedded;
    
    @Column(name = "created_time", nullable = false)
    private Instant createdTime = Instant.now();
    
    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime = Instant.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }
    
    @PrePersist
    protected void onCreate() {
        createdTime = Instant.now();
        updatedTime = Instant.now();
    }
    
    /**
     * Convert embedding vector to float array for easier manipulation.
     */
    public float[] getEmbeddingAsArray() {
        if (embedding == null) {
            return new float[0];
        }
        return embedding.toArray();
    }
    
    /**
     * Set embedding from float array.
     */
    public void setEmbeddingFromArray(float[] array) {
        if (array == null || array.length == 0) {
            this.embedding = null;
        } else {
            List<Float> list = new java.util.ArrayList<>();
            for (float value : array) {
                list.add(value);
            }
            this.embedding = new PGvector(list);
        }
    }
}
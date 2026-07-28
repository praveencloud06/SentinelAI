package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.domain.EmbeddingStatus;
import com.sentinelai.knowledge.infrastructure.persistence.KnowledgeEmbeddingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for knowledge embeddings with vector similarity search support.
 */
@Repository
public interface KnowledgeEmbeddingJpaRepository extends JpaRepository<KnowledgeEmbeddingEntity, UUID> {
    
    /**
     * Find embeddings by entity type and ID.
     */
    List<KnowledgeEmbeddingEntity> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    /**
     * Find embeddings by status.
     */
    List<KnowledgeEmbeddingEntity> findByStatus(EmbeddingStatus status);
    
    /**
     * Find embeddings by status with pagination.
     */
    Page<KnowledgeEmbeddingEntity> findByStatus(EmbeddingStatus status, Pageable pageable);
    
    /**
     * Find embeddings by entity type.
     */
    List<KnowledgeEmbeddingEntity> findByEntityType(String entityType);
    
    /**
     * Find embedding by content hash (for deduplication).
     */
    Optional<KnowledgeEmbeddingEntity> findByContentHash(String contentHash);
    
    /**
     * Find pending embeddings for processing.
     */
    @Query("SELECT e FROM KnowledgeEmbeddingEntity e WHERE e.status = 'PENDING' ORDER BY e.createdTime ASC")
    List<KnowledgeEmbeddingEntity> findPendingEmbeddings(Pageable pageable);
    
    /**
     * Find failed embeddings that are eligible for retry.
     */
    @Query("SELECT e FROM KnowledgeEmbeddingEntity e WHERE e.status = 'FAILED' AND e.lastEmbedded < :threshold ORDER BY e.lastEmbedded ASC")
    List<KnowledgeEmbeddingEntity> findFailedEmbeddingsForRetry(@Param("threshold") java.time.Instant threshold, Pageable pageable);
    
    /**
     * Vector similarity search using pgvector cosine distance.
     * Returns embeddings with their similarity scores.
     */
    @Query(value = """
        SELECT id, entity_type, entity_id, chunk_number, content, 
               embedding, embedding_model, embedding_version, status,
               content_hash, last_embedded, created_time, updated_time,
               1 - (embedding <=> :queryVector) as similarity
        FROM knowledge_embeddings
        WHERE status = 'COMPLETED'
          AND embedding IS NOT NULL
          AND (:entityType IS NULL OR entity_type = :entityType)
        ORDER BY embedding <=> :queryVector
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findBySimilarity(@Param("queryVector") String queryVector, 
                                      @Param("entityType") String entityType,
                                      @Param("limit") int limit);
    
    /**
     * Vector similarity search with minimum similarity threshold.
     */
    @Query(value = """
        SELECT id, entity_type, entity_id, chunk_number, content, 
               embedding, embedding_model, embedding_version, status,
               content_hash, last_embedded, created_time, updated_time,
               1 - (embedding <=> :queryVector) as similarity
        FROM knowledge_embeddings
        WHERE status = 'COMPLETED'
          AND embedding IS NOT NULL
          AND (:entityType IS NULL OR entity_type = :entityType)
          AND (embedding <=> :queryVector) < :maxDistance
        ORDER BY embedding <=> :queryVector
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findBySimilarityWithThreshold(@Param("queryVector") String queryVector,
                                                   @Param("entityType") String entityType,
                                                   @Param("maxDistance") double maxDistance,
                                                   @Param("limit") int limit);
    
    /**
     * Count embeddings by status.
     */
    long countByStatus(EmbeddingStatus status);
    
    /**
     * Count embeddings by entity type.
     */
    long countByEntityType(String entityType);
    
    /**
     * Delete old failed embeddings beyond a threshold.
     */
    @Query("DELETE FROM KnowledgeEmbeddingEntity e WHERE e.status = 'FAILED' AND e.lastEmbedded < :threshold")
    int deleteOldFailedEmbeddings(@Param("threshold") java.time.Instant threshold);
    
    /**
     * Find embeddings that need to be re-embedded (content hash mismatch or model version change).
     */
    @Query("SELECT e FROM KnowledgeEmbeddingEntity e WHERE e.status = 'COMPLETED' AND (e.embeddingModel <> :currentModel OR e.embeddingVersion <> :currentVersion)")
    List<KnowledgeEmbeddingEntity> findOutdatedEmbeddings(@Param("currentModel") String currentModel, 
                                                           @Param("currentVersion") String currentVersion);
}
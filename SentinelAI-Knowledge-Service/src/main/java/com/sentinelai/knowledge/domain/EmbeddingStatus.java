package com.sentinelai.knowledge.domain;

/**
 * Status of embedding generation for engineering entities.
 */
public enum EmbeddingStatus {
    /**
     * Entity is queued for embedding generation.
     */
    PENDING,
    
    /**
     * Embedding is currently being generated.
     */
    PROCESSING,
    
    /**
     * Embedding was successfully generated.
     */
    COMPLETED,
    
    /**
     * Embedding generation failed (will be retried).
     */
    FAILED,
    
    /**
     * Entity marked as not requiring embedding.
     */
    SKIPPED
}
package com.sentinelai.knowledge.retrieval;

import com.sentinelai.knowledge.domain.EmbeddingProvider;
import com.sentinelai.knowledge.domain.EmbeddingGenerationException;
import com.sentinelai.knowledge.infrastructure.persistence.KnowledgeEmbeddingEntity;
import com.sentinelai.knowledge.infrastructure.repository.KnowledgeEmbeddingJpaRepository;
import com.sentinelai.knowledge.infrastructure.vector.PgVectorOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for vector similarity search using pgvector.
 * Performs semantic search by finding similar embeddings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {
    
    private final KnowledgeEmbeddingJpaRepository repository;
    private final PgVectorOperations vectorOps;
    private final EmbeddingProvider embeddingProvider;
    
    /**
     * Search for similar embeddings by query text.
     * Generates embedding for query and finds similar documents.
     */
    public List<VectorSearchResult> search(String query, String entityType, int limit) {
        try {
            log.debug("Performing vector search for query: {}, entity type: {}, limit: {}", 
                    query, entityType, limit);
            
            // Generate embedding for query
            List<Float> queryEmbedding = embeddingProvider.generateEmbedding(query);
            float[] queryFloatArray = new float[queryEmbedding.size()];
            for (int i = 0; i < queryEmbedding.size(); i++) {
                queryFloatArray[i] = queryEmbedding.get(i);
            }
            String queryVector = vectorOps.arrayToVector(queryFloatArray);
            
            // Perform similarity search
            List<Object[]> results = repository.findBySimilarity(queryVector, entityType, limit);
            
            // Convert to search results
            List<VectorSearchResult> searchResults = new ArrayList<>();
            for (Object[] result : results) {
                KnowledgeEmbeddingEntity entity = (KnowledgeEmbeddingEntity) result[0];
                Double similarity = (Double) result[1];
                
                searchResults.add(VectorSearchResult.builder()
                        .entity(entity)
                        .similarity(similarity)
                        .build());
            }
            
            log.debug("Vector search returned {} results", searchResults.size());
            return searchResults;
            
        } catch (EmbeddingGenerationException e) {
            log.error("Failed to generate query embedding for vector search", e);
            return List.of();
        }
    }
    
    /**
     * Search with minimum similarity threshold.
     */
    public List<VectorSearchResult> searchWithThreshold(String query, String entityType, 
                                                         double maxDistance, int limit) {
        try {
            log.debug("Performing vector search with threshold for query: {}, max distance: {}", 
                    query, maxDistance);
            
            // Generate embedding for query
            List<Float> queryEmbedding = embeddingProvider.generateEmbedding(query);
            float[] queryFloatArray = new float[queryEmbedding.size()];
            for (int i = 0; i < queryEmbedding.size(); i++) {
                queryFloatArray[i] = queryEmbedding.get(i);
            }
            String queryVector = vectorOps.arrayToVector(queryFloatArray);
            
            // Perform similarity search with threshold
            List<Object[]> results = repository.findBySimilarityWithThreshold(
                    queryVector, entityType, maxDistance, limit);
            
            // Convert to search results
            List<VectorSearchResult> searchResults = new ArrayList<>();
            for (Object[] result : results) {
                KnowledgeEmbeddingEntity entity = (KnowledgeEmbeddingEntity) result[0];
                Double similarity = (Double) result[1];
                
                searchResults.add(VectorSearchResult.builder()
                        .entity(entity)
                        .similarity(similarity)
                        .build());
            }
            
            log.debug("Vector search with threshold returned {} results", searchResults.size());
            return searchResults;
            
        } catch (EmbeddingGenerationException e) {
            log.error("Failed to generate query embedding for vector search", e);
            return List.of();
        }
    }
    
    /**
     * Search by pre-computed embedding vector.
     */
    public List<VectorSearchResult> searchByVector(float[] queryVector, String entityType, int limit) {
        log.debug("Performing vector search by vector for entity type: {}, limit: {}", entityType, limit);
        
        String vectorString = vectorOps.arrayToVector(queryVector);
        List<Object[]> results = repository.findBySimilarity(vectorString, entityType, limit);
        
        List<VectorSearchResult> searchResults = new ArrayList<>();
        for (Object[] result : results) {
            KnowledgeEmbeddingEntity entity = (KnowledgeEmbeddingEntity) result[0];
            Double similarity = (Double) result[1];
            
            searchResults.add(VectorSearchResult.builder()
                    .entity(entity)
                    .similarity(similarity)
                    .build());
        }
        
        return searchResults;
    }
    
    /**
     * Check if vector search is available.
     */
    public boolean isAvailable() {
        try {
            // Check if embedding provider is available
            if (!embeddingProvider.isAvailable()) {
                return false;
            }
            
            // Check if there are any completed embeddings
            long completedCount = repository.countByStatus(
                    com.sentinelai.knowledge.domain.EmbeddingStatus.COMPLETED
            );
            
            return completedCount > 0;
            
        } catch (Exception e) {
            log.warn("Vector search availability check failed", e);
            return false;
        }
    }
    
    /**
     * Get statistics about vector search index.
     */
    public VectorSearchStats getStats() {
        long totalEmbeddings = repository.count();
        long completedEmbeddings = repository.countByStatus(
                com.sentinelai.knowledge.domain.EmbeddingStatus.COMPLETED
        );
        long pendingEmbeddings = repository.countByStatus(
                com.sentinelai.knowledge.domain.EmbeddingStatus.PENDING
        );
        long failedEmbeddings = repository.countByStatus(
                com.sentinelai.knowledge.domain.EmbeddingStatus.FAILED
        );
        
        return VectorSearchStats.builder()
                .totalEmbeddings(totalEmbeddings)
                .completedEmbeddings(completedEmbeddings)
                .pendingEmbeddings(pendingEmbeddings)
                .failedEmbeddings(failedEmbeddings)
                .available(isAvailable())
                .embeddingModel(embeddingProvider.getModel())
                .embeddingDimension(embeddingProvider.getDimension())
                .build();
    }
    
    /**
     * Result of vector similarity search.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VectorSearchResult {
        private KnowledgeEmbeddingEntity entity;
        private Double similarity;
    }
    
    /**
     * Statistics about vector search index.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VectorSearchStats {
        private long totalEmbeddings;
        private long completedEmbeddings;
        private long pendingEmbeddings;
        private long failedEmbeddings;
        private boolean available;
        private String embeddingModel;
        private int embeddingDimension;
    }
}
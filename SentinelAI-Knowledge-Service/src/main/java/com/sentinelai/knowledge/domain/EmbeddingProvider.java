package com.sentinelai.knowledge.domain;

import java.util.List;

/**
 * Abstraction for embedding generation providers.
 * Knowledge Service never directly depends on specific AI SDKs.
 */
public interface EmbeddingProvider {
    
    /**
     * Get the name of this provider.
     * @return provider name (e.g., "openrouter", "openai", "ollama", "azure-openai")
     */
    String getName();
    
    /**
     * Get the embedding model being used.
     * @return model name (e.g., "text-embedding-3-small", "nomic-embed-text")
     */
    String getModel();
    
    /**
     * Get the dimension of the embedding vectors.
     * @return vector dimension (e.g., 1536 for text-embedding-3-small)
     */
    int getDimension();
    
    /**
     * Generate embedding for a single text.
     * @param text the text to embed
     * @return embedding vector as list of floats
     * @throws EmbeddingGenerationException if generation fails
     */
    List<Float> generateEmbedding(String text) throws EmbeddingGenerationException;
    
    /**
     * Generate embeddings for multiple texts in batch.
     * @param texts list of texts to embed
     * @return list of embedding vectors
     * @throws EmbeddingGenerationException if generation fails
     */
    default List<List<Float>> generateEmbeddings(List<String> texts) throws EmbeddingGenerationException {
        return texts.stream()
                .map(this::generateEmbedding)
                .toList();
    }
    
    /**
     * Check if the provider is available and healthy.
     * @return true if provider is available
     */
    boolean isAvailable();
}
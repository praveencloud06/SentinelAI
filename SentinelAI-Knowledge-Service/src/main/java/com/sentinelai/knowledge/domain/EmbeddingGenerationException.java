package com.sentinelai.knowledge.domain;

/**
 * Exception thrown when embedding generation fails.
 */
public class EmbeddingGenerationException extends RuntimeException {
    
    private final String provider;
    private final boolean retryable;
    
    public EmbeddingGenerationException(String message) {
        super(message);
        this.provider = "unknown";
        this.retryable = true;
    }
    
    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.provider = "unknown";
        this.retryable = true;
    }
    
    public EmbeddingGenerationException(String provider, String message, boolean retryable) {
        super(message);
        this.provider = provider;
        this.retryable = retryable;
    }
    
    public EmbeddingGenerationException(String provider, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.provider = provider;
        this.retryable = retryable;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
}
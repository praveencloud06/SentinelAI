package com.sentinelai.knowledge.embedding.EmbeddingProviderImpl;

import com.sentinelai.knowledge.domain.EmbeddingGenerationException;
import com.sentinelai.knowledge.domain.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Ollama embedding provider implementation.
 * Uses local Ollama instance for embedding generation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaEmbeddingProvider implements EmbeddingProvider {
    
    private final WebClient webClient;
    
    @Value("${sentinelai.knowledge.embedding.ollama.base-url:http://localhost:11434}")
    private String baseUrl;
    
    @Value("${sentinelai.knowledge.embedding.ollama.model:nomic-embed-text}")
    private String model;
    
    @Override
    public String getName() {
        return "ollama";
    }
    
    @Override
    public String getModel() {
        return model;
    }
    
    @Override
    public int getDimension() {
        // Common Ollama embedding dimensions
        return switch (model) {
            case "nomic-embed-text" -> 768;
            case "mxbai-embed-large" -> 1024;
            case "all-minilm" -> 384;
            default -> 768;
        };
    }
    
    @Override
    public List<Float> generateEmbedding(String text) throws EmbeddingGenerationException {
        try {
            log.debug("Generating embedding via Ollama with model: {}", model);
            
            Map<String, Object> response = webClient.post()
                    .uri(baseUrl + "/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", model,
                            "prompt", text
                    ))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            if (response == null || !response.containsKey("embedding")) {
                throw new EmbeddingGenerationException(getName(), "Invalid response from Ollama", false);
            }
            
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) response.get("embedding");
            if (embedding == null) {
                throw new EmbeddingGenerationException(getName(), "Embedding vector is null", false);
            }
            
            return embedding.stream()
                    .map(Double::floatValue)
                    .toList();
            
        } catch (WebClientResponseException e) {
            log.error("Ollama API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is5xxServerError()) {
                throw new EmbeddingGenerationException(getName(), "Ollama server error: " + e.getMessage(), e, true);
            } else if (e.getStatusCode().is4xxClientError()) {
                throw new EmbeddingGenerationException(getName(), "Ollama client error: " + e.getMessage(), e, false);
            }
            throw new EmbeddingGenerationException(getName(), "Ollama error: " + e.getMessage(), e, true);
        } catch (Exception e) {
            log.error("Error generating embedding with Ollama", e);
            throw new EmbeddingGenerationException(getName(), "Failed to generate embedding: " + e.getMessage(), e, true);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("Ollama provider not available: {}", e.getMessage());
            return false;
        }
    }
}
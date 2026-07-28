package com.sentinelai.knowledge.embedding.EmbeddingProviderImpl;

import com.sentinelai.knowledge.domain.EmbeddingGenerationException;
import com.sentinelai.knowledge.domain.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * OpenRouter embedding provider implementation.
 * Routes to various models via OpenRouter API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenRouterEmbeddingProvider implements EmbeddingProvider {
    
    private final WebClient webClient;
    
    @Value("${sentinelai.knowledge.embedding.openrouter.api-key}")
    private String apiKey;
    
    @Value("${sentinelai.knowledge.embedding.openrouter.model:text-embedding-3-small}")
    private String model;
    
    @Value("${sentinelai.knowledge.embedding.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;
    
    @Override
    public String getName() {
        return "openrouter";
    }
    
    @Override
    public String getModel() {
        return model;
    }
    
    @Override
    public int getDimension() {
        // Default dimension for common models
        return switch (model) {
            case "text-embedding-3-small" -> 1536;
            case "text-embedding-3-large" -> 3072;
            case "nomic-embed-text" -> 768;
            default -> 1536;
        };
    }
    
    @Override
    public List<Float> generateEmbedding(String text) throws EmbeddingGenerationException {
        try {
            log.debug("Generating embedding via OpenRouter with model: {}", model);
            
            Map<String, Object> response = webClient.post()
                    .uri(baseUrl + "/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("HTTP-Referer", "https://sentinelai.com")
                    .header("X-Title", "SentinelAI")
                    .bodyValue(Map.of(
                            "model", model,
                            "input", text
                    ))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            if (response == null || !response.containsKey("data")) {
                throw new EmbeddingGenerationException(getName(), "Invalid response from OpenRouter", false);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data == null || data.isEmpty()) {
                throw new EmbeddingGenerationException(getName(), "No embedding data in response", false);
            }
            
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            if (embedding == null) {
                throw new EmbeddingGenerationException(getName(), "Embedding vector is null", false);
            }
            
            // Convert Double list to Float list
            return embedding.stream()
                    .map(Double::floatValue)
                    .toList();
            
        } catch (WebClientResponseException e) {
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is5xxServerError()) {
                throw new EmbeddingGenerationException(getName(), "OpenRouter server error: " + e.getMessage(), e, true);
            } else if (e.getStatusCode().is4xxClientError()) {
                throw new EmbeddingGenerationException(getName(), "OpenRouter client error: " + e.getMessage(), e, false);
            }
            throw new EmbeddingGenerationException(getName(), "OpenRouter error: " + e.getMessage(), e, true);
        } catch (Exception e) {
            log.error("Error generating embedding with OpenRouter", e);
            throw new EmbeddingGenerationException(getName(), "Failed to generate embedding: " + e.getMessage(), e, true);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri(baseUrl + "/models")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("OpenRouter provider not available: {}", e.getMessage());
            return false;
        }
    }
}
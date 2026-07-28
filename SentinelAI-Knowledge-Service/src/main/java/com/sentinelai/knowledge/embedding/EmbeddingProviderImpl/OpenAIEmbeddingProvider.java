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
 * OpenAI embedding provider implementation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    
    private final WebClient webClient;
    
    @Value("${sentinelai.knowledge.embedding.openai.api-key}")
    private String apiKey;
    
    @Value("${sentinelai.knowledge.embedding.openai.model:text-embedding-3-small}")
    private String model;
    
    @Value("${sentinelai.knowledge.embedding.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Override
    public String getName() {
        return "openai";
    }
    
    @Override
    public String getModel() {
        return model;
    }
    
    @Override
    public int getDimension() {
        return switch (model) {
            case "text-embedding-3-small" -> 1536;
            case "text-embedding-3-large" -> 3072;
            case "text-embedding-ada-002" -> 1536;
            default -> 1536;
        };
    }
    
    @Override
    public List<Float> generateEmbedding(String text) throws EmbeddingGenerationException {
        try {
            log.debug("Generating embedding via OpenAI with model: {}", model);
            
            Map<String, Object> response = webClient.post()
                    .uri(baseUrl + "/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(Map.of(
                            "model", model,
                            "input", text
                    ))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            
            if (response == null || !response.containsKey("data")) {
                throw new EmbeddingGenerationException(getName(), "Invalid response from OpenAI", false);
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
            
            return embedding.stream()
                    .map(Double::floatValue)
                    .toList();
            
        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is5xxServerError()) {
                throw new EmbeddingGenerationException(getName(), "OpenAI server error: " + e.getMessage(), e, true);
            } else if (e.getStatusCode().is4xxClientError()) {
                throw new EmbeddingGenerationException(getName(), "OpenAI client error: " + e.getMessage(), e, false);
            }
            throw new EmbeddingGenerationException(getName(), "OpenAI error: " + e.getMessage(), e, true);
        } catch (Exception e) {
            log.error("Error generating embedding with OpenAI", e);
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
            log.warn("OpenAI provider not available: {}", e.getMessage());
            return false;
        }
    }
}
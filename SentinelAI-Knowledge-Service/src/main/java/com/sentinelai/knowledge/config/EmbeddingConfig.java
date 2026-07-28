package com.sentinelai.knowledge.config;

import com.sentinelai.knowledge.domain.EmbeddingProvider;
import com.sentinelai.knowledge.embedding.EmbeddingProviderImpl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for embedding generation providers.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingConfig {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${sentinelai.knowledge.embedding.provider:openrouter}")
    private String provider;
    
    @Bean
    public WebClient embeddingWebClient() {
        return webClientBuilder.build();
    }
    
    @Bean
    public EmbeddingProvider embeddingProvider(WebClient embeddingWebClient) {
        log.info("Initializing embedding provider: {}", provider);
        
        return switch (provider.toLowerCase()) {
            case "openai" -> new OpenAIEmbeddingProvider(embeddingWebClient);
            case "ollama" -> new OllamaEmbeddingProvider(embeddingWebClient);
            case "azure-openai" -> new AzureOpenAIEmbeddingProvider(embeddingWebClient);
            case "openrouter" -> new OpenRouterEmbeddingProvider(embeddingWebClient);
            default -> {
                log.warn("Unknown embedding provider '{}', falling back to OpenRouter", provider);
                yield new OpenRouterEmbeddingProvider(embeddingWebClient);
            }
        };
    }
}
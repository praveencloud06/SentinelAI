package com.sentinel.ai.service;

import com.sentinel.ai.model.Log;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogEmbeddingOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(LogEmbeddingOrchestrator.class);
    private final EmbeddingService embeddingService;

    /**
     * Orchestrates the embedding step after a log is ingested.
     *
     * <p><b>Why separate orchestration from LogService?</b>
     * Keeping this as a separate service makes the ingestion workflow easier to evolve:
     * later we can run embedding asynchronously (queue/background job), add retries,
     * or add enrichment steps without bloating the controller/service that handles HTTP ingestion.</p>
     *
     * <p><b>Why catch exceptions?</b>
     * Embedding depends on an external runtime (Ollama + model availability). We don't want log ingestion
     * to fail just because Ollama is temporarily down or a model is not pulled yet.</p>
     */
    public void processLogEmbedding(Log log) {
        logger.info("Processing embedding for log ID: {}", log.getId());
        try {
            embeddingService.embedAndStore(log.getProcessedLog(), ""); // No resolution notes at ingestion
        } catch (Exception ex) {
            // Keep log ingestion resilient; embeddings can be retried once Ollama/model is available.
            logger.error("Embedding generation failed for log ID {}: {}", log.getId(), ex.getMessage(), ex);
        }
    }
}

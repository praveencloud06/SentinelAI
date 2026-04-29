package com.sentinel.ai.service;

import com.sentinel.ai.dto.LogUploadRequest;
import com.sentinel.ai.model.Log;
import com.sentinel.ai.repository.LogRepository;
import com.sentinel.ai.service.LogEmbeddingOrchestrator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    private final LogRepository logRepository;
    private final LogEmbeddingOrchestrator logEmbeddingOrchestrator;

    /**
     * Ingests a log and triggers embedding generation.
     *
     * <p><b>Business intent</b>: store the raw log for audit/debugging, store a normalized version for AI usage,
     * and generate an embedding so the log becomes searchable by "meaning" (semantic search).</p>
     *
     * <p><b>AI intent</b>: the processed/normalized log is what we send to Ollama for embedding generation
     * (see {@link EmbeddingService}). Normalization reduces noise (timestamps, extra whitespace) so embeddings
     * focus on the error signature and message.</p>
     */
    @Transactional
    public Log ingestLog(LogUploadRequest request) {
        logger.info("Ingesting log of type: {}", request.getLogType());
        String processed = preprocessLog(request.getContent(), request.getLogType());
        Log log = Log.builder()
                .rawLog(request.getContent())
                .processedLog(processed)
                .build();
        Log savedLog = logRepository.save(log);
        // Trigger embedding after saving log
        logEmbeddingOrchestrator.processLogEmbedding(savedLog);
        return savedLog;
    }

    private String preprocessLog(String content, String logType) {
        // Remove timestamps, normalize whitespace, extract error messages
        // (Simple placeholder logic, can be improved)
        String noTimestamps = content.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*? ", "");
        String normalized = noTimestamps.replaceAll("\\s+", " ").trim();
        // For now, just return normalized log
        return normalized;
    }
}

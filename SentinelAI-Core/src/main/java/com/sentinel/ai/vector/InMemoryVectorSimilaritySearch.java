package com.sentinel.ai.vector;

import com.sentinel.ai.dto.LogSearchResult;
import com.sentinel.ai.model.KnowledgeBaseEntry;
import com.sentinel.ai.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default (MVP) vector similarity search implementation.
 *
 * <p>Enterprise note: this loads all vectors into memory and compares them in Java.
 * It's intentionally isolated behind {@link VectorSimilaritySearch} so we can swap
 * the implementation later (e.g., Qdrant / pgvector SQL search).</p>
 */
@Service
@Primary
@RequiredArgsConstructor
public class InMemoryVectorSimilaritySearch implements VectorSimilaritySearch {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorSimilaritySearch.class);

    private final EmbeddingClient embeddingClient;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public List<LogSearchResult> search(String queryText, int topK) {
        logger.info("Vector similarity search for query: {}", queryText);

        float[] queryEmbedding = embeddingClient.embed(queryText);
        List<KnowledgeBaseEntry> allEntries = knowledgeBaseRepository.findAll();

        return allEntries.stream()
                .map(entry -> new LogSearchResult(
                        entry.getLogText(),
                        entry.getResolutionNotes(),
                        cosineSimilarity(queryEmbedding, entry.getEmbedding())
                ))
                .sorted(Comparator.comparingDouble(LogSearchResult::getSimilarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}


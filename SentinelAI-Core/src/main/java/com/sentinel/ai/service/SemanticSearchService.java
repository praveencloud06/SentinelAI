package com.sentinel.ai.service;

import com.sentinel.ai.dto.LogSearchResult;
import com.sentinel.ai.model.KnowledgeBaseEntry;
import com.sentinel.ai.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {
    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchService.class);
    private final EmbeddingModel embeddingModel;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    /**
     * Performs semantic search over stored logs.
     *
     * <p><b>Semantic search vs keyword search</b>
     * We convert both the user query and each stored log to embeddings (vectors) and then compute similarity
     * between vectors. This finds "meaningfully similar" logs even if the exact words differ.</p>
     *
     * <p><b>When does this call Ollama?</b>
     * We call Ollama once to generate an embedding for the query (via {@link EmbeddingModel#embed(String)}).
     * The stored embeddings were generated earlier during ingestion.</p>
     *
     * <p><b>Scalability note (MVP)</b>
     * This implementation loads all embeddings and compares them in Java. It's fine for demos / small data.
     * For production scale, you'd typically compute similarity in the database (e.g., pgvector) and only
     * return top-K rows.</p>
     */
    public List<LogSearchResult> search(String query, int topK) {
        logger.info("Semantic search for query: {}", query);

        // AI call: vectorize the user's query using the configured embedding model in Ollama.
        float[] queryEmbedding = embeddingModel.embed(query);

        // Fetch stored embeddings from the knowledge base.
        List<KnowledgeBaseEntry> allEntries = knowledgeBaseRepository.findAll();

        // Compute similarity in-memory and return the best matches.
        List<LogSearchResult> results = allEntries.stream()
                .map(entry -> new LogSearchResult(
                        entry.getLogText(),
                        entry.getResolutionNotes(),
                        cosineSimilarity(queryEmbedding, entry.getEmbedding())
                ))
                .sorted(Comparator.comparingDouble(LogSearchResult::getSimilarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        return results;
    }

    /**
     * Standard cosine similarity for two vectors.
     * Result ranges from -1..1, where 1 means "same direction" (very similar) and 0 means unrelated.
     */
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

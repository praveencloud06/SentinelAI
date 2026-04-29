package com.sentinel.ai.service;

import com.sentinel.ai.dto.LogSearchResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.sentinel.ai.vector.VectorSimilaritySearch;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {
    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchService.class);
    private final VectorSimilaritySearch vectorSimilaritySearch;

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
        return vectorSimilaritySearch.search(query, topK);
    }
}

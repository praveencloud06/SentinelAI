package com.sentinel.ai.vector;

import com.sentinel.ai.dto.LogSearchResult;

import java.util.List;

public interface VectorSimilaritySearch {
    List<LogSearchResult> search(String queryText, int topK);
}


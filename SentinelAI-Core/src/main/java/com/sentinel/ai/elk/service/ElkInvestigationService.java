package com.sentinel.ai.elk.service;

import com.sentinel.ai.elk.dto.ElkInvestigationRequest;
import com.sentinel.ai.elk.dto.ElkInvestigationResponse;

/**
 * Contract for the ELK Investigation workflow.
 *
 * <p>Implementations are expected to:
 * <ol>
 *   <li>Query Elasticsearch for matching logs</li>
 *   <li>Preprocess and deduplicate the log lines</li>
 *   <li>Build an AI investigation prompt via {@code ElkPromptBuilder}</li>
 *   <li>Call the existing AI Engine via {@code PythonAiMlService}</li>
 *   <li>Parse and return a structured {@link ElkInvestigationResponse}</li>
 * </ol>
 */
public interface ElkInvestigationService {

    /**
     * Run a full ELK investigation and return the AI-generated summary.
     *
     * @param request filters describing which logs to investigate
     * @return structured investigation result
     */
    ElkInvestigationResponse investigate(ElkInvestigationRequest request);
}

package com.sentinel.ai.elk.service;

import com.sentinel.ai.elk.client.ElkClient;
import com.sentinel.ai.elk.config.ElkProperties;
import com.sentinel.ai.elk.dto.ElkInvestigationRequest;
import com.sentinel.ai.elk.dto.ElkInvestigationResponse;
import com.sentinel.ai.elk.exception.ElkInvestigationException;
import com.sentinel.ai.elk.prompt.ElkPromptBuilder;
import com.sentinel.ai.service.PythonAiMlService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full ELK investigation pipeline:
 *
 * <pre>
 * Request → ElkClient (ES query)
 *         → ElkPromptBuilder (prompt construction)
 *         → PythonAiMlService (existing AI Engine – no new service created)
 *         → ElkResponseParser (structured response)
 * </pre>
 *
 * <p>{@code ElkClient} is injected as {@code Optional} so this service bean is
 * always created, even when {@code sentinelai.elk.enabled=false}, which allows
 * the Spring context to start cleanly and the controller to return a meaningful
 * error rather than a 404.
 */
@Service
@RequiredArgsConstructor
public class ElkInvestigationServiceImpl implements ElkInvestigationService {

    private static final Logger logger = LoggerFactory.getLogger(ElkInvestigationServiceImpl.class);

    /** Present only when {@code sentinelai.elk.enabled=true}. */
    private final Optional<ElkClient> elkClient;
    private final ElkProperties elkProperties;
    private final ElkPromptBuilder elkPromptBuilder;
    private final ElkResponseParser elkResponseParser;
    /** Reuses the EXISTING AI Engine integration – no new AI service. */
    private final PythonAiMlService pythonAiMlService;

    @Override
    public ElkInvestigationResponse investigate(ElkInvestigationRequest request) {
        logger.info("ELK investigation started – service={} severity={} timeframeMinutes={}",
                request.getService(), request.getSeverity(), request.getTimeframeMinutes());

        // Guard: feature flag check
        if (!elkProperties.isEnabled() || elkClient.isEmpty()) {
            logger.warn("ELK investigation requested but feature is disabled");
            throw new ElkInvestigationException(
                    "ELK integration is disabled. Set sentinelai.elk.enabled=true in " +
                    "application-local.yml and ensure Elasticsearch is running via elk-docker-compose.yml.");
        }

        // ── Step 1: Fetch logs from Elasticsearch ────────────────────────────
        List<String> logLines = elkClient.get().fetchLogs(
                request.getService(),
                request.getSeverity(),
                request.getTimeframeMinutes());

        if (logLines.isEmpty()) {
            logger.info("No matching logs found in ES for the given filters – skipping AI call");
            return ElkInvestigationResponse.builder()
                    .summary("No logs matching the specified filters were found in Elasticsearch.")
                    .probableRootCause("Cannot determine – no log data available for the selected timeframe.")
                    .impactedService(request.getService())
                    .recommendedAction(
                            "Verify the service name and severity are correct. " +
                            "Confirm that logs are being shipped to the '" +
                            elkProperties.getIndex() + "' index.")
                    .suspiciousLogs(Collections.emptyList())
                    .build();
        }

        // ── Step 2: Build AI investigation prompt ────────────────────────────
        String prompt = elkPromptBuilder.build(
                request.getService(),
                request.getSeverity(),
                logLines);
        logger.debug("Prompt built – chars={}", prompt.length());

        // ── Step 3: Call existing AI Engine ──────────────────────────────────
        // Reuses PythonAiMlService unchanged – no new AI service created.
        logger.info("Sending ELK investigation prompt to AI Engine (logs={})", logLines.size());
        String rawAiResponse = pythonAiMlService.analyzeLogWithAI(prompt);
        logger.debug("Raw AI response received – chars={}", rawAiResponse.length());

        // ── Step 4: Parse structured response ────────────────────────────────
        ElkInvestigationResponse result = elkResponseParser.parse(rawAiResponse, request.getService());

        logger.info("ELK investigation complete – service={} rootCause={}",
                request.getService(), result.getProbableRootCause());

        return result;
    }
}

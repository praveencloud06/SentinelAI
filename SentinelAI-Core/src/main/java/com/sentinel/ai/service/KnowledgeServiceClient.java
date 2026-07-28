package com.sentinel.ai.service;

import com.sentinel.ai.dto.ContextRetrievalRequest;
import com.sentinel.ai.dto.ContextRetrievalResponse;
import com.sentinel.ai.dto.ContextRetrieveRequest;
import com.sentinel.ai.dto.ContextRetrieveResult;
import com.sentinel.ai.dto.EngineeringContextResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeServiceClient.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {};

    @Value("${sentinelai.knowledge-service.enabled:false}")
    private boolean enabled;

    @Value("${sentinelai.knowledge-service.url:http://localhost:8090}")
    private String knowledgeServiceUrl;

    @Value("${sentinelai.knowledge-service.max-timeline-events:20}")
    private int maxTimelineEvents;

    @Value("${sentinelai.knowledge-service.request-timeout-ms:3000}")
    private int requestTimeoutMs;

    private final WebClient webClient = WebClient.create();

    // -------------------------------------------------------------------------
    // Semantic context retrieval for UI — POST /api/context/retrieve  (NEW)
    // -------------------------------------------------------------------------

    /**
     * Retrieves engineering context for the UI Engineering Context Explorer.
     * 
     * <p>This method is called by {@code ContextController} and acts as a
     * direct proxy to the Knowledge Service. It forwards the UI request
     * unchanged and returns the Knowledge Service response unchanged.
     * 
     * <p><b>Architecture:</b> Backend-for-Frontend (BFF) pattern
     * <pre>
     * UI → Core ContextController → this method → Knowledge Service
     * </pre>
     * 
     * <p><b>Graceful Degradation:</b> If Knowledge Service is unavailable,
     * returns an empty response with appropriate metadata so the UI can
     * display a friendly "no results" message.
     * 
     * @param request the context retrieval request from the UI
     * @return the context retrieval response from Knowledge Service
     */
    public ContextRetrievalResponse retrieveContextForUI(ContextRetrievalRequest request) {
        if (!enabled) {
            logger.debug("Knowledge Service disabled — returning empty context response");
            return buildEmptyResponse("Knowledge Service is disabled");
        }

        Instant start = Instant.now();
        logger.info("UI context retrieval started — url={} logsLength={} service={} environment={}",
                knowledgeServiceUrl, 
                request.getLogs() != null ? request.getLogs().length() : 0,
                request.getService(),
                request.getEnvironment());

        try {
            ContextRetrievalResponse response = webClient.post()
                    .uri(trimTrailingSlash(knowledgeServiceUrl) + "/api/context/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ContextRetrievalResponse.class)
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .block();

            long durationMs = Duration.between(start, Instant.now()).toMillis();

            if (response == null) {
                logger.warn("UI context retrieval returned null — returning empty response (durationMs={})", durationMs);
                return buildEmptyResponse("Knowledge Service returned no data");
            }

            logger.info("UI context retrieval completed — evidenceCount={} totalCandidates={} durationMs={}",
                    response.getEvidence() != null ? response.getEvidence().size() : 0,
                    response.getMetadata() != null ? response.getMetadata().getTotalCandidates() : 0,
                    durationMs);

            return response;

        } catch (Exception ex) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            logger.warn("UI context retrieval failed — returning empty response (durationMs={} reason={})", 
                    durationMs, ex.getMessage());
            return buildEmptyResponse("Knowledge Service unavailable: " + ex.getMessage());
        }
    }

    /**
     * Builds an empty context response for graceful degradation.
     */
    private ContextRetrievalResponse buildEmptyResponse(String message) {
        return ContextRetrievalResponse.builder()
                .summary("No engineering context available")
                .evidence(Collections.emptyList())
                .metadata(ContextRetrievalResponse.RetrievalMetadata.builder()
                        .count(0)
                        .totalCandidates(0)
                        .retrievalTimeMs(0L)
                        .confidence(0.0)
                        .searchStrategy("NONE")
                        .allSourcesAvailable(false)
                        .build())
                .build();
    }

    // -------------------------------------------------------------------------
    // Semantic context retrieval for RCA/ELK — POST /api/context/retrieve
    // -------------------------------------------------------------------------

    /**
     * Calls the Knowledge Service semantic retrieval endpoint and returns a
     * typed {@link ContextRetrieveResult} containing the context text and
     * associated metadata (summary, confidence, evidenceCount).
     *
     * <p>This method is used by <b>RCA and ELK Investigation</b> flows to
     * enrich AI prompts with engineering context. It converts the Knowledge
     * Service structured response into prompt-ready text.
     *
     * <p>Graceful fallback contract: this method <strong>never throws</strong>.
     * When the Knowledge Service is disabled, unreachable, or returns an
     * error, it logs the situation and returns a safe
     * {@link ContextRetrieveResult} with {@code retrieved=false} so callers
     * can continue with the existing prompt-building flow unchanged.</p>
     *
     * @param logText the raw log or investigation query to use for semantic search
     * @return a {@link ContextRetrieveResult}; never {@code null}
     */
    public ContextRetrieveResult retrieveContext(String logText) {
        if (!enabled) {
            logger.debug("Knowledge Service disabled — skipping semantic context retrieval");
            return ContextRetrieveResult.builder()
                    .retrieved(false)
                    .summary("")
                    .contextText("")
                    .build();
        }

        // Truncate very long log text to keep the request payload reasonable;
        // the Knowledge Service controls how much it actually uses.
        String query = logText != null ? logText : "";
        if (query.length() > 4000) {
            query = query.substring(0, 4000);
        }

        Instant start = Instant.now();
        logger.info("Knowledge context retrieval started — url={} queryLength={}",
                knowledgeServiceUrl, query.length());

        try {
            ContextRetrieveRequest requestBody = ContextRetrieveRequest.builder()
                    .query(query)
                    .build();

            ContextRetrieveResult result = webClient.post()
                    .uri(trimTrailingSlash(knowledgeServiceUrl) + "/api/context/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ContextRetrieveResult.class)
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .block();

            long durationMs = Duration.between(start, Instant.now()).toMillis();

            if (result == null) {
                logger.warn("Knowledge context retrieval returned null — falling back to empty context "
                        + "(durationMs={})", durationMs);
                return ContextRetrieveResult.builder().retrieved(false).build();
            }

            logger.info("Knowledge context retrieval completed — summary='{}' confidence={} "
                            + "evidenceCount={} durationMs={}",
                    result.getSummary(), result.getConfidence(),
                    result.getEvidenceCount(), durationMs);

            return result.toBuilder().retrieved(true).build();

        } catch (Exception ex) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            logger.warn("Knowledge context retrieval failed — falling back to empty context "
                    + "(durationMs={} reason={})", durationMs, ex.getMessage());
            return ContextRetrieveResult.builder()
                    .retrieved(false)
                    .summary("")
                    .contextText("")
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Existing legacy polling methods — UNCHANGED
    // -------------------------------------------------------------------------

    public EngineeringContextResponse fetchContext(String logText) {
        if (!enabled) {
            return EngineeringContextResponse.builder()
                    .enabled(false)
                    .available(false)
                    .message("Knowledge Service disabled")
                    .build();
        }

        try {
            List<Map<String, Object>> timeline = getList("/api/timeline?limit=" + Math.max(1, maxTimelineEvents));
            List<Map<String, Object>> deployments = getList("/api/deployments");
            List<Map<String, Object>> releases = getList("/api/releases");
            List<Map<String, Object>> jiraIssues = getList("/api/jira/issues");

            return EngineeringContextResponse.builder()
                    .enabled(true)
                    .available(true)
                    .message("Engineering context loaded from SentinelAI Knowledge Service")
                    .timeline(timeline)
                    .deployments(deployments)
                    .releases(releases)
                    .jiraIssues(jiraIssues)
                    .build();
        } catch (Exception ex) {
            logger.warn("Knowledge Service unavailable; continuing RCA without engineering context: {}", ex.getMessage());
            return EngineeringContextResponse.builder()
                    .enabled(true)
                    .available(false)
                    .message("Knowledge Service unavailable: " + ex.getMessage())
                    .build();
        }
    }

    public String toPromptContext(EngineeringContextResponse context) {
        if (context == null || !context.isEnabled() || !context.isAvailable()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Recent engineering timeline", context.getTimeline(), 8);
        appendSection(builder, "Recent deployments", context.getDeployments(), 5);
        appendSection(builder, "Recent releases", context.getReleases(), 5);
        appendSection(builder, "Relevant Jira issues", context.getJiraIssues(), 8);
        return builder.toString();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<Map<String, Object>> getList(String path) {
        List<Map<String, Object>> result = webClient.get()
                .uri(trimTrailingSlash(knowledgeServiceUrl) + path)
                .retrieve()
                .bodyToMono(LIST_OF_MAPS)
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .block();
        return result == null ? Collections.emptyList() : result;
    }

    private void appendSection(StringBuilder builder, String title, List<Map<String, Object>> items, int limit) {
        if (items == null || items.isEmpty()) {
            return;
        }
        builder.append(title).append(":\n");
        items.stream().limit(limit).forEach(item -> builder.append("- ").append(item).append("\n"));
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8090";
        }
        return value.replaceAll("/$", "");
    }
}

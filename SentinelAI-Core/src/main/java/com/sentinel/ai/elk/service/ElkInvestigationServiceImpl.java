package com.sentinel.ai.elk.service;

import com.sentinel.ai.dto.ContextRetrieveResult;
import com.sentinel.ai.dto.EngineeringContextResponse;
import com.sentinel.ai.elk.client.ElkClient;
import com.sentinel.ai.elk.config.ElkProperties;
import com.sentinel.ai.elk.dto.ElkInvestigationRequest;
import com.sentinel.ai.elk.dto.ElkInvestigationResponse;
import com.sentinel.ai.elk.exception.ElkInvestigationException;
import com.sentinel.ai.elk.prompt.ElkPromptBuilder;
import com.sentinel.ai.service.KnowledgeServiceClient;
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
 *         → KnowledgeServiceClient (semantic context retrieval — graceful fallback)
 *         → ElkPromptBuilder (prompt construction, optionally enriched)
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
    /** Knowledge Service client for semantic context enrichment. */
    private final KnowledgeServiceClient knowledgeServiceClient;

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

        // ── Step 2: Retrieve engineering context from Knowledge Service ───────
        // Combine log lines into a single query string for semantic search.
        // Cap the combined text so the HTTP request stays reasonable.
        String logQuery = buildLogQuery(request.getService(), logLines);
        ContextRetrieveResult semanticContext = knowledgeServiceClient.retrieveContext(logQuery);

        String contextText = null;
        EngineeringContextResponse engineeringContextMeta = null;
        String relatedJira = null;
        String relatedCommit = null;
        String relevantRunbook = null;
        
        if (semanticContext.isRetrieved() && !semanticContext.getContextText().isBlank()) {
            contextText = semanticContext.getContextText();
            
            // Extract engineering artifacts from context
            relatedJira = extractJiraTicket(contextText);
            relatedCommit = extractCommitHash(contextText);
            relevantRunbook = extractRunbookLink(contextText);
            
            engineeringContextMeta = EngineeringContextResponse.builder()
                    .enabled(true)
                    .available(true)
                    .message("Semantic context retrieved from SentinelAI Knowledge Service")
                    .summary(semanticContext.getSummary())
                    .confidence(semanticContext.getConfidence())
                    .evidenceCount(semanticContext.getEvidenceCount())
                    .build();
            logger.info("ELK investigation enriched with engineering context – "
                    + "confidence={} evidenceCount={} jira={} commit={}",
                    semanticContext.getConfidence(), semanticContext.getEvidenceCount(),
                    relatedJira, relatedCommit);
        } else {
            logger.info("No engineering context retrieved – proceeding without enrichment "
                    + "(retrieved={})", semanticContext.isRetrieved());
        }

        // ── Step 3: Build AI investigation prompt ────────────────────────────
        // Uses the overloaded build() that accepts optional context.
        // When contextText is null the output is identical to the original prompt.
        String prompt = elkPromptBuilder.build(
                request.getService(),
                request.getSeverity(),
                logLines,
                contextText);
        logger.debug("Prompt built – chars={} hasContext={}", prompt.length(), contextText != null);

        // ── Step 4: Call existing AI Engine ──────────────────────────────────
        // Reuses PythonAiMlService unchanged – no new AI service created.
        logger.info("Sending ELK investigation prompt to AI Engine (logs={})", logLines.size());
        String rawAiResponse = pythonAiMlService.analyzeLogWithAI(prompt);
        logger.debug("Raw AI response received – chars={}", rawAiResponse.length());

        // ── Step 5: Parse structured response ────────────────────────────────
        ElkInvestigationResponse result = elkResponseParser.parse(rawAiResponse, request.getService());

        // ── Step 6: Enrich with engineering context ──────────────────────────
        // Attach engineering context metadata so the UI can display it.
        // Field is null when context was unavailable, preserving backward compat.
        result.setEngineeringContext(engineeringContextMeta);
        
        // Populate enterprise fields from context if not provided by AI
        if (result.getRelatedJira() == null && relatedJira != null) {
            result.setRelatedJira(relatedJira);
        }
        if (result.getRelatedCommit() == null && relatedCommit != null) {
            result.setRelatedCommit(relatedCommit);
        }
        if (result.getRelevantRunbook() == null && relevantRunbook != null) {
            result.setRelevantRunbook(relevantRunbook);
        }
        
        // Set default confidence if not provided by AI
        if (result.getConfidence() == null && semanticContext.getConfidence() > 0) {
            result.setConfidence(semanticContext.getConfidence());
        }
        
        // Add query metadata
        result.setTotalLogsAnalyzed(logLines.size());

        logger.info("ELK investigation complete – service={} rootCause={} confidence={} jira={}",
                request.getService(), result.getProbableRootCause(), result.getConfidence(), result.getRelatedJira());

        return result;
    }

    /**
     * Combines the service name and a representative sample of log lines into
     * a single query string for Knowledge Service semantic search.
     * Limits output to 3000 characters to keep the HTTP request payload small.
     */
    private String buildLogQuery(String service, List<String> logLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("Service: ").append(service).append("\n");
        for (String line : logLines) {
            if (sb.length() >= 3000) {
                break;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Extracts Jira ticket ID from engineering context text.
     * Looks for patterns like "PAY-421", "ORD-334", etc.
     */
    private String extractJiraTicket(String contextText) {
        if (contextText == null) return null;
        // Match Jira-style ticket IDs (e.g., PAY-421, ORD-334)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b([A-Z]{2,10}-\\d+)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(contextText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extracts Git commit hash from engineering context text.
     * Looks for short hashes (7-8 chars) or full hashes (40 chars).
     */
    private String extractCommitHash(String contextText) {
        if (contextText == null) return null;
        // Match Git commit hashes (7-40 hex characters)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b([a-f0-9]{7,40})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(contextText);
        if (matcher.find()) {
            String hash = matcher.group(1);
            // Return short hash (first 8 chars)
            return hash.length() > 8 ? hash.substring(0, 8) : hash;
        }
        return null;
    }

    /**
     * Extracts runbook or documentation link from engineering context text.
     * Looks for Confluence URLs or mentions of runbooks.
     */
    private String extractRunbookLink(String contextText) {
        if (contextText == null) return null;
        // Look for mentions of runbooks or documentation
        if (contextText.toLowerCase().contains("runbook")) {
            // Extract the line containing "runbook"
            for (String line : contextText.split("\\n")) {
                if (line.toLowerCase().contains("runbook")) {
                    return line.trim();
                }
            }
        }
        // Look for Confluence or wiki URLs
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://[^\\s]+(?:confluence|wiki)[^\\s]*");
        java.util.regex.Matcher matcher = pattern.matcher(contextText);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }
}

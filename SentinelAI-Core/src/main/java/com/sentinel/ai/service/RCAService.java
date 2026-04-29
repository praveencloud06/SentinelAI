package com.sentinel.ai.service;

import com.sentinel.ai.dto.RCAResponse;
import com.sentinel.ai.model.KnowledgeBaseEntry;
import com.sentinel.ai.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RCAService {
    private static final Logger logger = LoggerFactory.getLogger(RCAService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final SemanticSearchService semanticSearchService;
    private final PythonAiMlService pythonAiMlService;

    @Value("${sentinelai.semantic-search.enabled:true}")
    private boolean semanticSearchEnabled;

    @Value("${sentinelai.limits.max-log-chars:20000}")
    private int maxLogChars;

    @Value("${sentinelai.limits.max-prompt-chars:30000}")
    private int maxPromptChars;

    /**
     * Runs a Root Cause Analysis (RCA) workflow by delegating RCA generation to the Python AI Engine.
     *
     * <p>This uses a simple form of <b>RAG (Retrieval-Augmented Generation)</b>:</p>
     * <ol>
     *   <li><b>Retrieve</b>: find the most similar past logs (semantic search) and use their resolutions as context</li>
     *   <li><b>Generate</b>: ask the LLM to produce a structured JSON RCA using the provided context</li>
     * </ol>
     *
     * <p><b>When does this call Ollama?</b></p>
     * <ul>
     *   <li>SemanticSearchService will call Ollama embeddings once for the input log (query embedding)</li>
     *   <li>This method calls the Python AI Engine once to produce the RCA JSON</li>
     * </ul>
     *
     * <p><b>Why ask for JSON?</b>
     * We want a predictable output contract for downstream usage (UI / Jira ticket / reports). In production,
     * you'd typically enforce JSON format more strictly (e.g., a structured output converter or schema).</p>
     */
    public RCAResponse analyze(String log) {
        logger.info("RCA analysis for log");
        List<String> limitWarnings = new ArrayList<>();

        String safeLog = log == null ? "" : log;
        if (safeLog.length() > maxLogChars) {
            limitWarnings.add("Input log truncated to " + maxLogChars + " characters");
            safeLog = safeLog.substring(0, maxLogChars);
        }

        // 1) RETRIEVE: get the top 3 most similar logs to build context (past incidents).
        // This uses embeddings + cosine similarity (semantic similarity).
        List<KnowledgeBaseEntry> similar = Collections.emptyList();
        if (semanticSearchEnabled) {
            try {
                similar = semanticSearchService.search(safeLog, 3).stream()
                        .map(result -> knowledgeBaseRepository.findAll().stream()
                                .filter(e -> e.getLogText().equals(result.getLogText()))
                                .findFirst().orElse(null))
                        .filter(e -> e != null)
                        .collect(Collectors.toList());
            } catch (Exception ex) {
                logger.warn("Semantic search unavailable; continuing without historical context: {}", ex.getMessage());
                similar = Collections.emptyList();
            }
        }

        StringBuilder context = new StringBuilder();
        for (KnowledgeBaseEntry entry : similar) {
            context.append("Log: ").append(entry.getLogText()).append("\n");
            context.append("Resolution: ").append(entry.getResolutionNotes()).append("\n---\n");
        }

        // 2) GENERATE: create an instruction prompt for the LLM.
        // We include: (a) the log to analyze, (b) similar incidents + resolutions.
        // Business intent: LLM proposes RCA based on patterns seen in previous incidents (and general knowledge),
        // while grounding the response with your own historical context.
        String promptText = "Given the following log and similar past incidents, analyze and return a JSON with keys: issue, rootCause, impactedService, recommendedFix.\n" +
                "Log to analyze: " + safeLog + "\n" +
                "Similar incidents:\n" + context;

        if (promptText.length() > maxPromptChars) {
            limitWarnings.add("Prompt truncated to " + maxPromptChars + " characters");
            promptText = promptText.substring(0, maxPromptChars);
        }

        // AI call: delegate RCA generation to the Python AI Engine (FastAPI) so we can switch providers there.
        String response = pythonAiMlService.analyzeLogWithAI(promptText);

        // For MVP, parse manually (should use a JSON parser in production).
        RCAResponse parsed = parseRCAResponse(response);
        if (!limitWarnings.isEmpty()) {
            List<String> merged = new ArrayList<>(parsed.getErrors() == null ? List.of() : parsed.getErrors());
            merged.addAll(limitWarnings);
            parsed.setErrors(merged);
        }
        return parsed;
    }

    private RCAResponse parseRCAResponse(String response) {
        if (response == null) {
            return new RCAResponse("", "", "", "", "", List.of("Empty AI response"));
        }

        // Prefer JSON parsing (AI-Engine returns structured fields).
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = (Map<String, Object>) (Map<?, ?>) OBJECT_MAPPER.readValue(response, Map.class);
            String issue = String.valueOf(parsed.getOrDefault("issue", ""));
            String rootCause = String.valueOf(parsed.getOrDefault("rootCause", ""));
            String impactedService = String.valueOf(parsed.getOrDefault("impactedService", ""));
            String recommendedFix = String.valueOf(parsed.getOrDefault("recommendedFix", ""));
            String provider = String.valueOf(parsed.getOrDefault("provider", ""));

            List<String> errors = new ArrayList<>();
            Object rawErrors = parsed.get("errors");
            if (rawErrors instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) errors.add(String.valueOf(item));
                }
            }

            return new RCAResponse(issue, rootCause, impactedService, recommendedFix, provider, errors);
        } catch (Exception ignored) {
            // Fall back to regex extraction (legacy / non-JSON provider outputs).
        }

        String issue = extractField(response, "issue");
        String rootCause = extractField(response, "rootCause");
        String impactedService = extractField(response, "impactedService");
        String recommendedFix = extractField(response, "recommendedFix");
        return new RCAResponse(issue, rootCause, impactedService, recommendedFix, "", List.of());
    }

    private String extractField(String json, String field) {
        String pattern = "\"" + field + "\"\s*:\s*\"(.*?)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
}

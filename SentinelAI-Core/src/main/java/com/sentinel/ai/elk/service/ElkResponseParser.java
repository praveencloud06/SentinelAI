package com.sentinel.ai.elk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.ai.elk.dto.ElkInvestigationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parses the raw string returned by the AI Engine into a structured
 * {@link ElkInvestigationResponse}.
 *
 * <p>Parsing strategy (first match wins):
 * <ol>
 *   <li>Strip optional markdown code fences the LLM may add</li>
 *   <li>Parse as JSON and map ELK-specific fields
 *       ({@code summary}, {@code probableRootCause}, {@code impactedService},
 *       {@code recommendedAction}, {@code suspiciousLogs})</li>
 *   <li>Fall back to legacy RCA fields
 *       ({@code issue} → summary, {@code rootCause} → probableRootCause,
 *       {@code recommendedFix} → recommendedAction)</li>
 *   <li>Fall back to treating the entire response as the summary when JSON
 *       parsing fails or no known keys are present</li>
 * </ol>
 */
@Component
public class ElkResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(ElkResponseParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * @param aiResponse       raw string from {@code PythonAiMlService}
     * @param requestedService service name from the original request (used as fallback)
     * @return structured response – never null
     */
    public ElkInvestigationResponse parse(String aiResponse, String requestedService) {
        if (aiResponse == null || aiResponse.isBlank()) {
            logger.warn("Empty AI response – returning fallback");
            return fallback(requestedService, "AI Engine returned an empty response.");
        }

        String cleaned = stripCodeFences(aiResponse);

        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(
                    cleaned, new TypeReference<Map<String, Object>>() {});

            // Accept both ELK field names and the AI Engine's standard RCA field names
            String summary = coalesce(
                    stringOrNull(parsed, "summary"),
                    stringOrNull(parsed, "issue"));
            String probableRootCause = coalesce(
                    stringOrNull(parsed, "probableRootCause"),
                    stringOrNull(parsed, "rootCause"));
            String impactedService = coalesce(
                    stringOrNull(parsed, "impactedService"),
                    requestedService);
            String recommendedAction = coalesce(
                    stringOrNull(parsed, "recommendedAction"),
                    stringOrNull(parsed, "recommendedFix"),
                    "Review the suspicious logs for further clues.");
            List<String> suspiciousLogs = listOrEmpty(parsed, "suspiciousLogs");

            // Fallback to raw text if JSON contained no recognisable fields
            if (summary == null && probableRootCause == null) {
                logger.debug("JSON parsed but no expected keys found – treating as plain text summary");
                return fallback(requestedService, cleaned);
            }

            return ElkInvestigationResponse.builder()
                    .summary(summary != null ? summary : "See suspicious logs for details.")
                    .probableRootCause(probableRootCause != null ? probableRootCause : "Could not determine root cause.")
                    .impactedService(impactedService)
                    .recommendedAction(recommendedAction)
                    .suspiciousLogs(suspiciousLogs)
                    .build();

        } catch (Exception e) {
            logger.warn("Could not parse AI response as JSON – treating as plain text. error={}", e.getMessage());
            return fallback(requestedService, cleaned);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ElkInvestigationResponse fallback(String service, String rawText) {
        return ElkInvestigationResponse.builder()
                .summary(rawText)
                .probableRootCause("Could not determine root cause automatically.")
                .impactedService(service)
                .recommendedAction("Review the AI summary above and investigate manually.")
                .suspiciousLogs(Collections.emptyList())
                .build();
    }

    /**
     * Removes markdown code fences ({@code ```json ... ```}) that some LLMs add
     * despite prompt instructions.
     */
    private String stripCodeFences(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.strip();
    }

    @SuppressWarnings("unchecked")
    private List<String> listOrEmpty(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof List<?> list) ? (List<String>) list : Collections.emptyList();
    }

    private String stringOrNull(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : null;
    }

    /** Returns the first non-null value from the provided candidates. */
    @SafeVarargs
    private <T> T coalesce(T... values) {
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }
}

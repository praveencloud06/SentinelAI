package com.sentinel.ai.elk.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Constructs a structured AI investigation prompt from preprocessed
 * Elasticsearch log entries.
 *
 * <p>The prompt instructs the AI to:
 * <ul>
 *   <li>Summarise the incident in 1-2 sentences</li>
 *   <li>Identify suspicious patterns</li>
 *   <li>Determine the probable root cause</li>
 *   <li>Recommend a concrete next action for the on-call engineer</li>
 *   <li>Return output as a valid JSON object only (no markdown fences)</li>
 * </ul>
 *
 * <p>Duplicate log lines are removed (order preserved) before the prompt is
 * built to reduce noise and keep token usage low.
 */
@Component
public class ElkPromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ElkPromptBuilder.class);

    /**
     * Builds the investigation prompt.
     *
     * @param service   target service name
     * @param severity  severity level that was queried (displayed in context header)
     * @param logLines  raw message strings fetched from Elasticsearch
     * @return fully formatted prompt ready to send to the AI Engine
     */
    public String build(String service, String severity, List<String> logLines) {
        // Deduplicate while preserving insertion order so repeated noise lines
        // don't inflate the prompt or distort AI attention.
        List<String> deduplicated = new ArrayList<>(new LinkedHashSet<>(logLines));

        logger.debug("Building prompt – service={} severity={} raw={} deduped={}",
                service, severity, logLines.size(), deduplicated.size());

        // Number each log line so the AI can reference specific entries.
        StringBuilder logBlock = new StringBuilder();
        for (int i = 0; i < deduplicated.size(); i++) {
            logBlock.append(String.format("[%d] %s%n", i + 1, deduplicated.get(i)));
        }

        return """
                You are an expert SRE (Site Reliability Engineer) performing root cause analysis on production logs.

                === INVESTIGATION CONTEXT ===
                Service:      %s
                Severity:     %s
                Logs Fetched: %d (duplicates removed)

                === LOG ENTRIES ===
                %s
                === TASK ===
                Analyze ALL log entries above and respond ONLY with a valid JSON object in this exact structure:

                {
                  "issue": "<concise 1-2 sentence description of the overall incident>",
                  "rootCause": "<most likely root cause inferred from log patterns>",
                  "impactedService": "<service most affected by this incident>",
                  "recommendedFix": "<specific, actionable next step for the on-call engineer>",
                  "suspiciousLogs": ["<verbatim log line>", "<up to 5 most suspicious entries>"]
                }

                Rules:
                - Output ONLY the JSON object. No markdown code fences, no prose before or after.
                - Keep each string value concise (under 200 characters).
                - suspiciousLogs must be verbatim lines copied from the LOG ENTRIES section, maximum 5.
                - If root cause cannot be determined from the logs, state that clearly.
                """.formatted(service, severity.toUpperCase(), deduplicated.size(), logBlock.toString());
    }
}

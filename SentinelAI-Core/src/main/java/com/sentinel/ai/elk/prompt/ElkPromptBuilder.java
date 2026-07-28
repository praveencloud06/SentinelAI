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
     * Builds the investigation prompt without engineering context.
     *
     * <p>This is the <strong>original method</strong> — signature and output
     * are completely unchanged. Existing callers continue to work as before.</p>
     *
     * @param service   target service name
     * @param severity  severity level that was queried (displayed in context header)
     * @param logLines  raw message strings fetched from Elasticsearch
     * @return fully formatted prompt ready to send to the AI Engine
     */
    public String build(String service, String severity, List<String> logLines) {
        return build(service, severity, logLines, null);
    }

    /**
     * Builds the investigation prompt, optionally enriched with engineering
     * context retrieved from the Knowledge Service.
     *
     * <p>When {@code engineeringContext} is non-blank the prompt gains a
     * {@code === RELEVANT ENGINEERING CONTEXT ===} section between the log
     * entries and the task instructions, exactly as specified:</p>
     *
     * <pre>
     * Incident Logs
     * Relevant Engineering Context   ← inserted verbatim, unmodified
     * Instructions
     * </pre>
     *
     * <p>When {@code engineeringContext} is {@code null} or blank this method
     * produces <em>exactly the same output</em> as the single-argument
     * {@link #build(String, String, List)} so the fallback path is free.</p>
     *
     * @param service             target service name
     * @param severity            severity level that was queried
     * @param logLines            raw message strings fetched from Elasticsearch
     * @param engineeringContext  verbatim context text from Knowledge Service,
     *                            or {@code null} / blank to omit the section
     * @return fully formatted prompt ready to send to the AI Engine
     */
    public String build(String service, String severity, List<String> logLines,
                        String engineeringContext) {

        // Deduplicate while preserving insertion order so repeated noise lines
        // don't inflate the prompt or distort AI attention.
        List<String> deduplicated = new ArrayList<>(new LinkedHashSet<>(logLines));

        logger.debug("Building prompt – service={} severity={} raw={} deduped={} hasContext={}",
                service, severity, logLines.size(), deduplicated.size(),
                engineeringContext != null && !engineeringContext.isBlank());

        // Number each log line so the AI can reference specific entries.
        StringBuilder logBlock = new StringBuilder();
        for (int i = 0; i < deduplicated.size(); i++) {
            logBlock.append(String.format("[%d] %s%n", i + 1, deduplicated.get(i)));
        }

        // Build the optional engineering context section.
        // Content is included verbatim — not summarised or modified.
        String contextSection = "";
        if (engineeringContext != null && !engineeringContext.isBlank()) {
            contextSection = """

                    === RELEVANT ENGINEERING CONTEXT ===
                    (Retrieved from SentinelAI Knowledge Service — use this to enrich root cause analysis)

                    %s
                    """.formatted(engineeringContext.trim());
        }

        return """
                You are an expert SRE (Site Reliability Engineer) performing root cause analysis on production logs.

                === INVESTIGATION CONTEXT ===
                Service:      %s
                Severity:     %s
                Logs Fetched: %d (duplicates removed)

                === LOG ENTRIES ===
                %s%s=== TASK ===
                Analyze ALL log entries above and respond ONLY with a valid JSON object in this exact structure:

                {
                  "summary": "<concise 1-2 sentence executive summary>",
                  "probableRootCause": "<most likely root cause inferred from log patterns>",
                  "impactedService": "<service most affected>",
                  "recommendedAction": "<specific, actionable next step>",
                  "suspiciousLogs": ["<verbatim log line>", "<up to 5 most suspicious entries>"],
                  "confidence": <integer 0-100 representing confidence in the analysis>,
                  "relatedJira": "<Jira ticket ID if found in context, e.g. PAY-421>",
                  "relatedCommit": "<Git commit hash if found in context>",
                  "relevantRunbook": "<Link or name of runbook if found in context>",
                  "nextSteps": ["<Step 1>", "<Step 2>", "<Step 3>"],
                  "timeline": [
                    {"timestamp": "<ISO timestamp or relative time>", "description": "<What happened>"},
                    {"timestamp": "<ISO timestamp or relative time>", "description": "<What happened>"}
                  ],
                  "escalate": <boolean: true if this requires immediate escalation>,
                  "escalationReason": "<Why escalation is needed, if escalate=true>"
                }

                Rules:
                - Output ONLY the JSON object. No markdown code fences, no prose before or after.
                - All string values should be concise (under 200 characters).
                - suspiciousLogs must be verbatim lines from LOG ENTRIES section, maximum 5.
                - confidence: 80+ = high confidence, 50-79 = medium, <50 = low confidence in analysis.
                - timeline: Extract key events from logs in chronological order (2-5 events).
                - nextSteps: Provide 2-4 concrete action items for remediation.
                - escalate: Set to true only if this is a critical production incident requiring immediate attention.
                - Extract relatedJira, relatedCommit, relevantRunbook from ENGINEERING CONTEXT if available.
                - If root cause cannot be determined, state that clearly and set confidence accordingly.
                """.formatted(service, severity.toUpperCase(), deduplicated.size(),
                logBlock.toString(), contextSection);
    }
}

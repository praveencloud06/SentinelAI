package com.sentinel.ai.elk.dto;

import com.sentinel.ai.dto.EngineeringContextResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured AI-generated investigation result returned by
 * {@code POST /api/elk-investigation/search}.
 * 
 * <p><b>Enterprise Observability Response</b> - Enhanced with additional
 * fields for confidence, related artifacts, timeline, and escalation.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElkInvestigationResponse {

    // ══════════════════════════════════════════════════════════════════════════
    // Core Investigation Results (Original)
    // ══════════════════════════════════════════════════════════════════════════

    /** High-level summary of the incident detected in the logs. */
    private String summary;

    /** AI-inferred probable root cause of the issue. */
    private String probableRootCause;

    /** Service most likely impacted by the detected anomaly. */
    private String impactedService;

    /** Suggested next action for the on-call engineer. */
    private String recommendedAction;

    /** Subset of raw log lines flagged as suspicious by the AI. */
    private List<String> suspiciousLogs;

    /**
     * Engineering context metadata from the Knowledge Service.
     *
     * <p>Populated when the Knowledge Service is enabled and reachable.
     * {@code null} when the Knowledge Service is disabled or unavailable
     * so that existing clients that do not read this field are unaffected.</p>
     */
    private EngineeringContextResponse engineeringContext;

    // ══════════════════════════════════════════════════════════════════════════
    // Enterprise Enhancements (New - All Optional)
    // ══════════════════════════════════════════════════════════════════════════

    /** 
     * AI confidence level in the root cause analysis (0-100).
     * Used for color-coded badges in UI: 80+ High (red), 50-79 Medium (orange), <50 Low (green).
     */
    private Integer confidence;

    /** Related deployment information (e.g., "v2.1.5 deployed 2h ago"). */
    private String relatedDeployment;

    /** Related Jira ticket ID (e.g., "PAY-421"). */
    private String relatedJira;

    /** Related Git commit hash or reference (e.g., "abc123de"). */
    private String relatedCommit;

    /** Link to relevant runbook or documentation (e.g., "Payment Service Runbook"). */
    private String relevantRunbook;

    /** 
     * Chronological timeline of events leading to the incident.
     * Each item can be a string or a structured object with timestamp + description.
     */
    private List<Object> timeline;

    /** 
     * Structured next steps for remediation.
     * Displayed as bulleted list in UI.
     */
    private List<String> nextSteps;

    /** 
     * Whether this incident requires escalation.
     * If true, displays warning box in UI.
     */
    private Boolean escalate;

    /** Reason why escalation is recommended (shown if escalate=true). */
    private String escalationReason;

    /** 
     * Number of matching log entries found in Elasticsearch.
     * Useful for UI display (e.g., "Analyzed 127 log entries").
     */
    private Integer totalLogsAnalyzed;

    /** 
     * Query execution time in milliseconds.
     * Can be used for performance metrics display.
     */
    private Long queryTimeMs;
}

package com.sentinel.ai.elk.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Incoming request for the ELK Investigation endpoint.
 *
 * <p>Maps to {@code POST /api/elk-investigation/search}.
 * 
 * <p><b>Backward Compatibility:</b> All fields except basic filters are optional.
 * Existing clients using only service/severity/timeframeMinutes will continue to work.
 */
@Data
public class ElkInvestigationRequest {

    // ══════════════════════════════════════════════════════════════════════════
    // Core Filters (Original - Backward Compatible)
    // ══════════════════════════════════════════════════════════════════════════

    /** Name of the service whose logs should be investigated (e.g. {@code payment-service}). */
    private String service;

    /** 
     * Severity level to filter on (e.g. {@code ERROR}, {@code WARN}). 
     * @deprecated Use {@code severityList} for multi-select support.
     */
    private String severity;

    /**
     * How many minutes back from now the query should look.
     * Defaults to 30 minutes if not supplied.
     * Ignored if {@code customTimeFrom} and {@code customTimeTo} are provided.
     */
    @Positive(message = "timeframeMinutes must be a positive integer")
    private int timeframeMinutes = 30;

    // ══════════════════════════════════════════════════════════════════════════
    // Enterprise Filters (New - All Optional)
    // ══════════════════════════════════════════════════════════════════════════

    /** Environment filter (e.g. {@code Production}, {@code Staging}, {@code QA}, {@code Development}). */
    private String environment;

    /** Application filter (optional dropdown selection). */
    private String application;

    /** 
     * Multi-select severity levels (e.g. {@code ["ERROR", "WARN"]}). 
     * Takes precedence over {@code severity} if provided.
     */
    private List<String> severityList;

    /** Free-text search across log messages (e.g. {@code timeout}, {@code NullPointerException}). */
    private String searchText;

    /** Request ID for tracing specific requests. */
    private String requestId;

    /** Correlation ID for distributed tracing. */
    private String correlationId;

    /** Host or Pod name filter (e.g. {@code payment-pod-7f8a9b}, {@code host-123}). */
    private String host;

    /** Deployment version filter (e.g. {@code v2.1.5}, {@code release-2024-01}). */
    private String deploymentVersion;

    /** Maximum number of logs to retrieve. Defaults to 100 if not specified. */
    @Positive(message = "maxLogs must be a positive integer")
    private Integer maxLogs = 100;

    /** 
     * Whether to include engineering context (GitHub, Jira, Confluence, etc.) in the analysis.
     * Defaults to {@code true}.
     */
    private Boolean includeContext = true;

    // ══════════════════════════════════════════════════════════════════════════
    // Custom Time Range (New - All Optional)
    // ══════════════════════════════════════════════════════════════════════════

    /** 
     * Custom time range start (ISO 8601 format). 
     * If provided with {@code customTimeTo}, overrides {@code timeframeMinutes}.
     */
    private Instant customTimeFrom;

    /** 
     * Custom time range end (ISO 8601 format).
     * If provided with {@code customTimeFrom}, overrides {@code timeframeMinutes}.
     */
    private Instant customTimeTo;
}

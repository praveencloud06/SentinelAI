package com.sentinel.ai.elk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured AI-generated investigation result returned by
 * {@code POST /api/elk-investigation/search}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElkInvestigationResponse {

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
}

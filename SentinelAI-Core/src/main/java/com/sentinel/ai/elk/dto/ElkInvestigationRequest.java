package com.sentinel.ai.elk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Incoming request for the ELK Investigation endpoint.
 *
 * <p>Maps to {@code POST /api/elk-investigation/search}.
 */
@Data
public class ElkInvestigationRequest {

    /** Name of the service whose logs should be investigated (e.g. {@code payment-service}). */
    @NotBlank(message = "service must not be blank")
    private String service;

    /** Severity level to filter on (e.g. {@code ERROR}, {@code WARN}). */
    @NotBlank(message = "severity must not be blank")
    private String severity;

    /**
     * How many minutes back from now the query should look.
     * Defaults to 30 minutes if not supplied.
     */
    @Positive(message = "timeframeMinutes must be a positive integer")
    private int timeframeMinutes = 30;
}

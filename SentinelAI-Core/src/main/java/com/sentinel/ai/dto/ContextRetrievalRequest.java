package com.sentinel.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for Engineering Context Explorer UI.
 * 
 * <p>This is used by the {@code POST /api/context/retrieve} endpoint in
 * {@code ContextController}, which acts as a proxy between the UI and the
 * Knowledge Service.
 * 
 * <p>This DTO is separate from {@link ContextRetrieveRequest}, which is used
 * internally by RCA and ELK Investigation flows. The UI version has additional
 * filter fields that the internal version does not need.
 * 
 * <p><b>Contract:</b> Must match the UI TypeScript type {@code ContextRetrievalRequest}
 * in {@code sentinelai-ui/src/types/context/context.types.js}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrievalRequest {

    /**
     * Error logs or incident description to search against.
     * This is the primary input for semantic similarity search.
     */
    @NotBlank(message = "Logs are required")
    private String logs;

    /**
     * Optional service name filter (e.g., "payment-service").
     * When provided, results are filtered to match this service.
     */
    private String service;

    /**
     * Optional environment filter (e.g., "prod", "staging").
     */
    private String environment;

    /**
     * Optional repository name filter (e.g., "sentinelai/api").
     */
    private String repository;

    /**
     * Optional deployment identifier filter.
     */
    private String deployment;

    /**
     * Optional commit hash filter.
     */
    private String commit;

    /**
     * Optional incident timestamp for temporal filtering.
     * Engineering events near this time are ranked higher.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant time;

    /**
     * Maximum number of evidence items to return.
     * Default: 10, Range: 1-50
     */
    @Builder.Default
    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 50, message = "maxResults must not exceed 50")
    private Integer maxResults = 10;

    /**
     * Minimum relevance score threshold (0.0 - 1.0).
     * Evidence below this score is filtered out.
     * Default: 0.3
     */
    @Builder.Default
    @Min(value = 0, message = "minScore must be at least 0")
    @Max(value = 1, message = "minScore must not exceed 1")
    private Double minScore = 0.3;

    /**
     * Include historical incidents in the search results.
     */
    @Builder.Default
    private Boolean includeHistoricalIncidents = true;

    /**
     * Include runbooks in the search results.
     */
    @Builder.Default
    private Boolean includeRunbooks = true;

    /**
     * Include architecture documents in the search results.
     */
    @Builder.Default
    private Boolean includeArchitectureDocs = true;
}

package com.sentinelai.knowledge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for context retrieval API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrievalRequest {
    
    private String logs;
    
    // Optional filters
    private String service;
    private String environment;
    private String repository;
    private String deployment;
    private String commit;
    private Instant time;
    
    // Retrieval options
    @Builder.Default
    private Integer maxResults = 10;
    
    @Builder.Default
    private Double minScore = 0.3;
    
    // Search options
    private Boolean includeHistoricalIncidents;
    private Boolean includeRunbooks;
    private Boolean includeArchitectureDocs;
}

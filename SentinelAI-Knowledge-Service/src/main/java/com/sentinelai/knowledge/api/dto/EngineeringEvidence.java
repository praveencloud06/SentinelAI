package com.sentinelai.knowledge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Unified model for engineering evidence returned by context retrieval.
 * All integrations map to this common format for consistent API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringEvidence {
    
    /**
     * Type of evidence (COMMIT, JIRA_ISSUE, DEPLOYMENT, etc.)
     */
    private String type;
    
    /**
     * Human-readable title
     */
    private String title;
    
    /**
     * Brief summary of the evidence
     */
    private String summary;
    
    /**
     * Combined ranking score (0-1)
     */
    private Double score;
    
    /**
     * Explanation of why this evidence was selected
     */
    private String reason;
    
    /**
     * Additional metadata specific to the evidence type
     */
    private Map<String, Object> metadata;
    
    /**
     * Source system (GITHUB, JIRA, CONFLUENCE, etc.)
     */
    private String sourceSystem;
    
    /**
     * ID in the source system
     */
    private String sourceId;
    
    /**
     * Direct link to the source
     */
    private String link;
    
    // Individual signal scores for transparency
    
    /**
     * Vector similarity score (0-1)
     */
    private Double vectorSimilarityScore;
    
    /**
     * Relationship graph score (0-1)
     */
    private Double relationshipScore;
    
    /**
     * Timeline proximity score (0-1)
     */
    private Double timelineScore;
    
    /**
     * Service match score (0-1)
     */
    private Double serviceMatchScore;
    
    /**
     * Environment match score (0-1)
     */
    private Double environmentMatchScore;
    
    /**
     * Recency score (0-1)
     */
    private Double recencyScore;
    
    /**
     * Exact match score (0-1)
     */
    private Double exactMatchScore;
}
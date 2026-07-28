package com.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Engineering Context Explorer UI.
 * 
 * <p>Returned by the {@code POST /api/context/retrieve} endpoint in
 * {@code ContextController}. This response structure matches the Knowledge
 * Service response and is passed through unchanged to the UI.
 * 
 * <p><b>Contract:</b> Must match the UI TypeScript type {@code ContextRetrievalResponse}
 * in {@code sentinelai-ui/src/types/context/context.types.js}.
 * 
 * <p>This is separate from {@link ContextRetrieveResult}, which is used
 * internally by RCA/ELK flows and contains pre-formatted prompt text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrievalResponse {

    /**
     * Human-readable summary of the retrieved engineering context.
     * Example: "Found 5 related deployments and 3 Jira issues within 2 hours of incident"
     */
    private String summary;

    /**
     * List of ranked engineering evidence items.
     * Sorted by relevance score (highest first).
     */
    private List<EngineeringEvidence> evidence;

    /**
     * Metadata about the retrieval operation.
     */
    private RetrievalMetadata metadata;

    /**
     * Individual piece of engineering evidence from various sources.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineeringEvidence {
        
        /** Unique identifier for this evidence item */
        private String id;
        
        /** Type of evidence: GITHUB_COMMIT, DEPLOYMENT, JIRA_ISSUE, CONFLUENCE_DOC, etc. */
        private String type;
        
        /** Source system: GitHub, Jira, Confluence, Jenkins, etc. */
        private String source;
        
        /** Title or subject of the evidence */
        private String title;
        
        /** Brief summary or description */
        private String summary;
        
        /** Full content or details (may be large) */
        private String content;
        
        /** Relevance score (0.0 - 1.0) */
        private Double score;
        
        /** Human-readable explanation of why this was selected */
        private String reason;
        
        /** URL to view the original item */
        private String url;
        
        /** ISO-8601 timestamp of when this item was created */
        private String timestamp;
        
        /** Service or repository this evidence relates to */
        private String service;
        
        /** Additional metadata specific to this evidence type */
        private Object metadata;
    }

    /**
     * Metadata about the context retrieval operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievalMetadata {
        
        /** Number of evidence items returned */
        private Integer count;
        
        /** Total number of candidates evaluated before filtering */
        private Integer totalCandidates;
        
        /** Time taken to retrieve context (milliseconds) */
        private Long retrievalTimeMs;
        
        /** Overall confidence in the retrieved context (0.0 - 1.0) */
        private Double confidence;
        
        /** Search strategy used: SEMANTIC, HYBRID, KEYWORD */
        private String searchStrategy;
        
        /** Whether all requested sources were available */
        private Boolean allSourcesAvailable;
    }
}

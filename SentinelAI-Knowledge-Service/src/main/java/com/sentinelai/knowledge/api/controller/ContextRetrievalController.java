package com.sentinelai.knowledge.api.controller;

import com.sentinelai.knowledge.api.dto.ContextRetrievalRequest;
import com.sentinelai.knowledge.api.dto.ContextRetrievalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STUB Controller for Context Retrieval API (V2 Feature - Not Yet Fully Implemented)
 * 
 * This is a temporary stub that returns demo data to prevent UI errors.
 * Full implementation with vector search, ranking engine, etc. is pending.
 */
@Slf4j
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextRetrievalController {
    
    @PostMapping("/retrieve")
    public ContextRetrievalResponse retrieveContext(@RequestBody ContextRetrievalRequest request) {
        log.info("Context retrieval request received (STUB): logs={}", 
                request.getLogs() != null ? request.getLogs().substring(0, Math.min(100, request.getLogs().length())) : "null");
        
        // TODO: Implement actual context retrieval with vector search and ranking
        // For now, return a stub response to prevent UI errors
        
        List<EngineeringEvidence> evidence = new ArrayList<>();
        
        // Check if the query mentions specific keywords and return relevant stub data
        String query = request.getLogs() != null ? request.getLogs().toLowerCase() : "";
        
        if (query.contains("kafka") || query.contains("consumer") || query.contains("lag")) {
            evidence.add(createStubEvidence(
                "JIRA_ISSUE",
                "ORD-334: Kafka consumer lag after deployment",
                "Order service Kafka consumer lag increased to 50K messages after v3.2.1 deployment.",
                0.85,
                "JIRA",
                "ORD-334"
            ));
            
            evidence.add(createStubEvidence(
                "GITHUB_COMMIT",
                "Update Kafka consumer configuration for reliability",
                "Commit 789ghi012jkl modified consumer settings",
                0.75,
                "GITHUB",
                "789ghi012jkl"
            ));
        }
        
        if (query.contains("payment") || query.contains("hikaricp") || query.contains("timeout") || query.contains("database")) {
            evidence.add(createStubEvidence(
                "JIRA_ISSUE",
                "PAY-421: Payment processing timeout errors",
                "Multiple payment transactions failing with HikariCP connection pool exhaustion after v2.1.5 deployment.",
                0.90,
                "JIRA",
                "PAY-421"
            ));
            
            evidence.add(createStubEvidence(
                "GITHUB_COMMIT",
                "Increase database timeout for long-running transactions",
                "Commit abc123def456 modified database timeout configuration",
                0.80,
                "GITHUB",
                "abc123def456"
            ));
            
            evidence.add(createStubEvidence(
                "CONFLUENCE_DOCUMENT",
                "Payment Service Database Configuration Guide",
                "HikariCP connection pool settings and troubleshooting guide",
                0.70,
                "CONFLUENCE",
                "CONF-123456"
            ));
            
            evidence.add(createStubEvidence(
                "JENKINS_DEPLOYMENT",
                "payment-service-deploy-prod #1245",
                "Deployed v2.1.5 with database timeout changes",
                0.65,
                "JENKINS",
                "1245"
            ));
        }
        
        // If no specific matches, return generic message
        if (evidence.isEmpty()) {
            log.warn("No matching stub data found for query: {}", query);
        }
        
        RetrievalMetadata metadata = new RetrievalMetadata();
        metadata.setTotalCandidates(evidence.size());
        metadata.setFilteredResults(evidence.size());
        metadata.setRetrievalTimeMs(15L);
        metadata.setQueryEmbeddingModel("stub-no-embedding");
        metadata.setAllSourcesAvailable(true);
        
        ContextRetrievalResponse response = new ContextRetrievalResponse();
        response.setSummary(evidence.isEmpty() 
            ? "No engineering context found (stub endpoint)" 
            : "Found " + evidence.size() + " related items (stub endpoint - full implementation pending)");
        response.setEvidence(evidence);
        response.setMetadata(metadata);
        
        log.info("Returning {} evidence items (STUB)", evidence.size());
        return response;
    }
    
    @PostMapping("/history")
    public List<Map<String, Object>> getHistory() {
        log.info("Context history requested (STUB)");
        // Return empty history for now
        return new ArrayList<>();
    }
    
    private EngineeringEvidence createStubEvidence(
            String type, String title, String summary, 
            double score, String sourceSystem, String sourceId) {
        EngineeringEvidence evidence = new EngineeringEvidence();
        evidence.setType(type);
        evidence.setTitle(title);
        evidence.setSummary(summary);
        evidence.setScore(score);
        evidence.setReason("Keyword match in stub endpoint");
        evidence.setSourceSystem(sourceSystem);
        evidence.setSourceId(sourceId);
        evidence.setLink("#");
        evidence.setTimestamp(Instant.now().toString());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("stub", true);
        metadata.put("note", "Full context retrieval with vector search not yet implemented");
        evidence.setMetadata(metadata);
        
        return evidence;
    }
    
    // Inner classes for response structure
    
    public static class EngineeringEvidence {
        private String id;
        private String type;
        private String source;
        private String title;
        private String summary;
        private Double score;
        private String reason;
        private String url;
        private String timestamp;
        private String sourceSystem;
        private String sourceId;
        private String link;
        private Map<String, Object> metadata;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class RetrievalMetadata {
        private Integer totalCandidates;
        private Integer filteredResults;
        private Long retrievalTimeMs;
        private String queryEmbeddingModel;
        private Boolean allSourcesAvailable;
        private Double confidence;
        private String searchStrategy;
        private Integer count;
        
        // Getters and setters
        public Integer getTotalCandidates() { return totalCandidates; }
        public void setTotalCandidates(Integer totalCandidates) { this.totalCandidates = totalCandidates; }
        public Integer getFilteredResults() { return filteredResults; }
        public void setFilteredResults(Integer filteredResults) { this.filteredResults = filteredResults; }
        public Long getRetrievalTimeMs() { return retrievalTimeMs; }
        public void setRetrievalTimeMs(Long retrievalTimeMs) { this.retrievalTimeMs = retrievalTimeMs; }
        public String getQueryEmbeddingModel() { return queryEmbeddingModel; }
        public void setQueryEmbeddingModel(String queryEmbeddingModel) { this.queryEmbeddingModel = queryEmbeddingModel; }
        public Boolean getAllSourcesAvailable() { return allSourcesAvailable; }
        public void setAllSourcesAvailable(Boolean allSourcesAvailable) { this.allSourcesAvailable = allSourcesAvailable; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        public String getSearchStrategy() { return searchStrategy; }
        public void setSearchStrategy(String searchStrategy) { this.searchStrategy = searchStrategy; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}

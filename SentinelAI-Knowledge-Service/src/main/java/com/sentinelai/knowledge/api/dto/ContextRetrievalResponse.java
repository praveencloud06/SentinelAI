package com.sentinelai.knowledge.api.dto;

import com.sentinelai.knowledge.api.controller.ContextRetrievalController.EngineeringEvidence;
import com.sentinelai.knowledge.api.controller.ContextRetrievalController.RetrievalMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for context retrieval API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrievalResponse {
    private String summary;
    private List<EngineeringEvidence> evidence;
    private RetrievalMetadata metadata;
}

package com.sentinel.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body sent to the Knowledge Service semantic context endpoint.
 *
 * <pre>POST /api/context/retrieve</pre>
 *
 * <p>Independent of all existing chat/RCA DTOs. Only the {@code query}
 * field is required; the Knowledge Service is responsible for all
 * semantic retrieval logic.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextRetrieveRequest {

    /**
     * The query text used for semantic similarity search inside the
     * Knowledge Service.  Typically the raw log text or a concise
     * summary of the incident being investigated.
     */
    @JsonProperty("query")
    private String query;
}

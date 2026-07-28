package com.sentinel.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Typed response from the Knowledge Service semantic context endpoint.
 *
 * <pre>POST /api/context/retrieve</pre>
 *
 * <p>All fields are nullable / have safe defaults so that partial responses
 * from the Knowledge Service (or a future schema extension) never cause
 * deserialization failures in Core.</p>
 *
 * <p>This DTO is the single source of truth for the context retrieval
 * contract between Core and the Knowledge Service.  It must not be merged
 * with {@link EngineeringContextResponse} — that class carries legacy
 * polling data; this one carries semantic-search results.</p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextRetrieveResult {

    /**
     * Human-readable summary of the retrieved engineering context.
     * Example: "Deployment v2.5 introduced connection pool changes."
     */
    @JsonProperty("summary")
    @Builder.Default
    private String summary = "";

    /**
     * Confidence score (0–100) assigned by the Knowledge Service to the
     * retrieved context relative to the query.
     */
    @JsonProperty("confidence")
    @Builder.Default
    private int confidence = 0;

    /**
     * Number of knowledge artifacts (commits, issues, deployments, docs)
     * that contributed to the retrieved context.
     */
    @JsonProperty("evidenceCount")
    @Builder.Default
    private int evidenceCount = 0;

    /**
     * The full engineering context text ready to be appended to an AI
     * prompt.  The Knowledge Service formats this; Core must include it
     * verbatim without summarising or modifying it.
     */
    @JsonProperty("contextText")
    @Builder.Default
    private String contextText = "";

    /**
     * Whether the retrieval was successful.  Set to {@code false} by the
     * client-side fallback when the Knowledge Service is unavailable.
     */
    @JsonProperty("retrieved")
    @Builder.Default
    private boolean retrieved = false;
}

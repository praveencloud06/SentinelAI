package com.sentinel.ai.controller;

import com.sentinel.ai.dto.ContextRetrievalRequest;
import com.sentinel.ai.dto.ContextRetrievalResponse;
import com.sentinel.ai.service.KnowledgeServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Engineering Context API Controller
 * 
 * <p>Acts as a Backend-for-Frontend (BFF) proxy between the React UI and
 * the Knowledge Service. This ensures:
 * <ul>
 *   <li>Single backend entry point for the UI (no direct microservice calls)</li>
 *   <li>Centralized authentication and authorization</li>
 *   <li>No CORS issues</li>
 *   <li>Consistent logging and request validation</li>
 *   <li>Future caching can be implemented here</li>
 *   <li>UI remains decoupled from internal microservice architecture</li>
 * </ul>
 * 
 * <p>This controller is specifically for the <b>Engineering Context Explorer</b>
 * feature in the UI. It is separate from the RCA and ELK Investigation flows,
 * which use {@code KnowledgeServiceClient} directly within their service layers.
 * 
 * <p><b>Architecture:</b>
 * <pre>
 * UI Engineering Context Explorer
 *   ↓
 *   POST /api/context/retrieve (this controller)
 *   ↓
 *   KnowledgeServiceClient.retrieveContextForUI()
 *   ↓
 *   Knowledge Service POST /api/context/retrieve
 *   ↓
 *   Response forwarded back to UI (unchanged)
 * </pre>
 */
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextController {

    private static final Logger logger = LoggerFactory.getLogger(ContextController.class);

    private final KnowledgeServiceClient knowledgeServiceClient;

    /**
     * Retrieve engineering context based on logs or incident description.
     * 
     * <p>This endpoint is called by the <b>Engineering Context Explorer</b>
     * page in the React UI. It proxies the request to the Knowledge Service
     * and returns the response unchanged.
     * 
     * <p>The response includes:
     * <ul>
     *   <li>Summary of retrieved context</li>
     *   <li>Ranked engineering evidence (commits, deployments, Jira issues, etc.)</li>
     *   <li>Retrieval metadata (confidence scores, timing, etc.)</li>
     * </ul>
     * 
     * @param request context retrieval request from UI
     * @return engineering context response from Knowledge Service
     */
    @PostMapping("/retrieve")
    public ResponseEntity<ContextRetrievalResponse> retrieveContext(
            @Valid @RequestBody ContextRetrievalRequest request) {

        logger.info("Context retrieval request received from UI – logsLength={} service={} environment={}",
                request.getLogs() != null ? request.getLogs().length() : 0,
                request.getService(),
                request.getEnvironment());

        // Delegate to Knowledge Service via the client
        ContextRetrievalResponse response = knowledgeServiceClient.retrieveContextForUI(request);

        logger.info("Context retrieval completed – evidenceCount={} totalCandidates={}",
                response.getEvidence() != null ? response.getEvidence().size() : 0,
                response.getMetadata() != null ? response.getMetadata().getTotalCandidates() : 0);

        return ResponseEntity.ok(response);
    }

    /**
     * Get search history for context retrieval.
     * 
     * <p>Returns empty list for now - history tracking not yet implemented.
     * This prevents UI errors when the history feature is used.
     * 
     * @return empty list (history tracking pending)
     */
    @PostMapping("/history")
    public ResponseEntity<?> getHistory() {
        logger.debug("Context history requested - returning empty list (not yet implemented)");
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }
}

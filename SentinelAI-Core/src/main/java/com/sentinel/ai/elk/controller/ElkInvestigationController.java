package com.sentinel.ai.elk.controller;

import com.sentinel.ai.elk.dto.ElkInvestigationRequest;
import com.sentinel.ai.elk.dto.ElkInvestigationResponse;
import com.sentinel.ai.elk.service.ElkInvestigationService;
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
 * REST controller exposing the ELK Investigation API.
 *
 * <p>This is a NEW controller and does NOT affect any existing RCA or
 * log-upload endpoints.
 *
 * <pre>
 * POST /api/elk-investigation/search
 * </pre>
 */
@RestController
@RequestMapping("/api/elk-investigation")
@RequiredArgsConstructor
public class ElkInvestigationController {

    private static final Logger logger = LoggerFactory.getLogger(ElkInvestigationController.class);

    private final ElkInvestigationService elkInvestigationService;

    /**
     * Triggers a live ELK investigation by querying Elasticsearch with the
     * provided filters and running AI-powered root cause analysis.
     *
     * @param request service, severity, and timeframe filters
     * @return structured investigation summary
     */
    @PostMapping("/search")
    public ResponseEntity<ElkInvestigationResponse> investigate(
            @Valid @RequestBody ElkInvestigationRequest request) {

        logger.info("ELK investigation request – service={} severity={} timeframeMinutes={}",
                request.getService(), request.getSeverity(), request.getTimeframeMinutes());

        ElkInvestigationResponse response = elkInvestigationService.investigate(request);
        return ResponseEntity.ok(response);
    }
}

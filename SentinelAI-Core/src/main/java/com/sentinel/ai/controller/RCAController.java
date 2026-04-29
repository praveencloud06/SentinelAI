package com.sentinel.ai.controller;

import com.sentinel.ai.dto.RCARequest;
import com.sentinel.ai.dto.RCAResponse;
import com.sentinel.ai.service.RCAService;
import com.sentinel.ai.service.JiraService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rca")
@RequiredArgsConstructor
public class RCAController {
    private static final Logger logger = LoggerFactory.getLogger(RCAController.class);
    private final RCAService rcaService;
    private final JiraService jiraService;

    @PostMapping("/analyze")
    public ResponseEntity<RCAResponse> analyze(@RequestBody RCARequest request) {
        logger.info("Received RCA analyze request");
        RCAResponse response = rcaService.analyze(request.getLog());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jira")
    public ResponseEntity<String> createJira(@RequestParam String projectKey, @RequestBody RCAResponse rca) {
        logger.info("Creating Jira ticket for RCA");
        String summary = rca.getIssue();
        String description = "Root Cause: " + rca.getRootCause() + "\nImpacted Service: " + rca.getImpactedService() + "\nRecommended Fix: " + rca.getRecommendedFix();
        String jiraKey = jiraService.createJiraTicket(projectKey, summary, description);
        return ResponseEntity.ok(jiraKey);
    }
}

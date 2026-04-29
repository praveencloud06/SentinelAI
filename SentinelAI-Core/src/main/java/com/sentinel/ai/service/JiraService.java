package com.sentinel.ai.service;

import com.sentinel.ai.config.JiraConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JiraService {
    private static final Logger logger = LoggerFactory.getLogger(JiraService.class);
    private final JiraConfig jiraConfig;
    private final RestTemplate restTemplate;

    public String createJiraTicket(String projectKey, String summary, String description) {
        if (jiraConfig.getJiraBaseUrl() == null || jiraConfig.getJiraBaseUrl().isBlank()) {
            throw new IllegalStateException("Jira is not configured. Set sentinelai.jira.base-url, sentinelai.jira.username, sentinelai.jira.api-token.");
        }
        String url = jiraConfig.getJiraBaseUrl() + "/rest/api/2/issue";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = jiraConfig.getJiraUsername() + ":" + jiraConfig.getJiraApiToken();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> fields = new HashMap<>();
        Map<String, Object> project = new HashMap<>();
        project.put("key", projectKey);
        fields.put("project", project);
        fields.put("summary", summary);
        fields.put("description", description);
        Map<String, Object> issueType = new HashMap<>();
        issueType.put("name", "Task");
        fields.put("issuetype", issueType);
        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            logger.info("Jira ticket created: {}", response.getBody().get("key"));
            return response.getBody().get("key").toString();
        } else {
            logger.error("Failed to create Jira ticket: {}", response.getStatusCode());
            throw new RuntimeException("Jira ticket creation failed");
        }
    }
}

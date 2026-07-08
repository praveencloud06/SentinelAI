package com.sentinel.ai.service;

import com.sentinel.ai.dto.EngineeringContextResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeServiceClient.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {
            };

    @Value("${sentinelai.knowledge-service.enabled:false}")
    private boolean enabled;

    @Value("${sentinelai.knowledge-service.url:http://localhost:8090}")
    private String knowledgeServiceUrl;

    @Value("${sentinelai.knowledge-service.max-timeline-events:20}")
    private int maxTimelineEvents;

    @Value("${sentinelai.knowledge-service.request-timeout-ms:3000}")
    private int requestTimeoutMs;

    private final WebClient webClient = WebClient.create();

    public EngineeringContextResponse fetchContext(String logText) {
        if (!enabled) {
            return EngineeringContextResponse.builder()
                    .enabled(false)
                    .available(false)
                    .message("Knowledge Service disabled")
                    .build();
        }

        try {
            List<Map<String, Object>> timeline = getList("/api/timeline?limit=" + Math.max(1, maxTimelineEvents));
            List<Map<String, Object>> deployments = getList("/api/deployments");
            List<Map<String, Object>> releases = getList("/api/releases");
            List<Map<String, Object>> jiraIssues = getList("/api/jira/issues");

            return EngineeringContextResponse.builder()
                    .enabled(true)
                    .available(true)
                    .message("Engineering context loaded from SentinelAI Knowledge Service")
                    .timeline(timeline)
                    .deployments(deployments)
                    .releases(releases)
                    .jiraIssues(jiraIssues)
                    .build();
        } catch (Exception ex) {
            logger.warn("Knowledge Service unavailable; continuing RCA without engineering context: {}", ex.getMessage());
            return EngineeringContextResponse.builder()
                    .enabled(true)
                    .available(false)
                    .message("Knowledge Service unavailable: " + ex.getMessage())
                    .build();
        }
    }

    public String toPromptContext(EngineeringContextResponse context) {
        if (context == null || !context.isEnabled() || !context.isAvailable()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Recent engineering timeline", context.getTimeline(), 8);
        appendSection(builder, "Recent deployments", context.getDeployments(), 5);
        appendSection(builder, "Recent releases", context.getReleases(), 5);
        appendSection(builder, "Relevant Jira issues", context.getJiraIssues(), 8);
        return builder.toString();
    }

    private List<Map<String, Object>> getList(String path) {
        List<Map<String, Object>> result = webClient.get()
                .uri(trimTrailingSlash(knowledgeServiceUrl) + path)
                .retrieve()
                .bodyToMono(LIST_OF_MAPS)
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .block();
        return result == null ? Collections.emptyList() : result;
    }

    private void appendSection(StringBuilder builder, String title, List<Map<String, Object>> items, int limit) {
        if (items == null || items.isEmpty()) {
            return;
        }
        builder.append(title).append(":\n");
        items.stream().limit(limit).forEach(item -> builder.append("- ").append(item).append("\n"));
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8090";
        }
        return value.replaceAll("/$", "");
    }
}

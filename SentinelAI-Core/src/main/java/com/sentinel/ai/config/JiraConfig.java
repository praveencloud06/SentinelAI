package com.sentinel.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class JiraConfig {
    @Value("${sentinelai.jira.base-url:}")
    private String jiraBaseUrl;

    @Value("${sentinelai.jira.username:}")
    private String jiraUsername;

    @Value("${sentinelai.jira.api-token:}")
    private String jiraApiToken;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getJiraBaseUrl() { return jiraBaseUrl; }
    public String getJiraUsername() { return jiraUsername; }
    public String getJiraApiToken() { return jiraApiToken; }
}

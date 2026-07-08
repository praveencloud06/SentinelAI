package com.sentinelai.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "sentinelai.knowledge.platforms")
public class ExternalPlatformProperties {
    private GitPlatform github = new GitPlatform();
    private GitPlatform gitlab = new GitPlatform();
    private JiraPlatform jira = new JiraPlatform();
    private ConfluencePlatform confluence = new ConfluencePlatform();
    private JenkinsPlatform jenkins = new JenkinsPlatform();

    public GitPlatform getGithub() {
        return github;
    }

    public void setGithub(GitPlatform github) {
        this.github = github;
    }

    public GitPlatform getGitlab() {
        return gitlab;
    }

    public void setGitlab(GitPlatform gitlab) {
        this.gitlab = gitlab;
    }

    public JiraPlatform getJira() {
        return jira;
    }

    public void setJira(JiraPlatform jira) {
        this.jira = jira;
    }

    public ConfluencePlatform getConfluence() {
        return confluence;
    }

    public void setConfluence(ConfluencePlatform confluence) {
        this.confluence = confluence;
    }

    public JenkinsPlatform getJenkins() {
        return jenkins;
    }

    public void setJenkins(JenkinsPlatform jenkins) {
        this.jenkins = jenkins;
    }

    public static class BasePlatform {
        private boolean enabled = false;
        private String baseUrl;
        private String username;
        private String apiToken;
        private String webhookSecret;
        private String webhookTokenHeader = "x-sentinelai-webhook-token";
        private String defaultTenantId = "default";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getWebhookTokenHeader() {
            return webhookTokenHeader;
        }

        public void setWebhookTokenHeader(String webhookTokenHeader) {
            this.webhookTokenHeader = webhookTokenHeader;
        }

        public String getDefaultTenantId() {
            return defaultTenantId;
        }

        public void setDefaultTenantId(String defaultTenantId) {
            this.defaultTenantId = defaultTenantId;
        }
    }

    public static class GitPlatform extends BasePlatform {
        private List<String> repositories = new ArrayList<>();

        public List<String> getRepositories() {
            return repositories;
        }

        public void setRepositories(List<String> repositories) {
            this.repositories = repositories;
        }
    }

    public static class JiraPlatform extends BasePlatform {
        private List<String> projectKeys = new ArrayList<>();

        public List<String> getProjectKeys() {
            return projectKeys;
        }

        public void setProjectKeys(List<String> projectKeys) {
            this.projectKeys = projectKeys;
        }
    }

    public static class ConfluencePlatform extends BasePlatform {
        private List<String> spaceKeys = new ArrayList<>();

        public List<String> getSpaceKeys() {
            return spaceKeys;
        }

        public void setSpaceKeys(List<String> spaceKeys) {
            this.spaceKeys = spaceKeys;
        }
    }

    public static class JenkinsPlatform extends BasePlatform {
        private List<String> jobNames = new ArrayList<>();

        public List<String> getJobNames() {
            return jobNames;
        }

        public void setJobNames(List<String> jobNames) {
            this.jobNames = jobNames;
        }
    }
}

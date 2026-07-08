package com.sentinelai.knowledge.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelai.knowledge.api.dto.SyncResponse;
import com.sentinelai.knowledge.config.ExternalPlatformProperties;
import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.RelationshipType;
import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.domain.SyncStatus;
import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ConfluenceDocumentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import com.sentinelai.knowledge.infrastructure.persistence.PullRequestEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ReleaseEntity;
import com.sentinelai.knowledge.infrastructure.persistence.RepositoryEntity;
import com.sentinelai.knowledge.infrastructure.repository.CommitMetadataJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.ConfluenceDocumentJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.DeploymentJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.EngineeringEventJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.JiraIssueJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.PullRequestJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.ReleaseJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WebhookIngestionService {
    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b");

    private final ObjectMapper objectMapper;
    private final RepositoryJpaRepository repositoryRepository;
    private final CommitMetadataJpaRepository commitRepository;
    private final PullRequestJpaRepository pullRequestRepository;
    private final ReleaseJpaRepository releaseRepository;
    private final JiraIssueJpaRepository jiraIssueRepository;
    private final ConfluenceDocumentJpaRepository confluenceDocumentRepository;
    private final DeploymentJpaRepository deploymentRepository;
    private final EngineeringEventJpaRepository eventRepository;
    private final RelationshipService relationshipService;
    private final ExternalPlatformProperties platformProperties;

    @Transactional
    public SyncResponse ingest(SourceSystem sourceSystem, Map<String, String> headers, Map<String, Object> payload) {
        Instant startedAt = Instant.now();
        ExternalPlatformProperties.BasePlatform platform = platform(sourceSystem);
        validateWebhookToken(sourceSystem, headers, platform);
        JsonNode root = objectMapper.valueToTree(payload);
        List<EngineeringEventEntity> events = switch (sourceSystem) {
            case GITHUB, GITLAB -> ingestGit(sourceSystem, headers, root, platform.getDefaultTenantId());
            case JIRA -> ingestJira(root, platform.getDefaultTenantId());
            case CONFLUENCE -> ingestConfluence(root, platform.getDefaultTenantId());
            case JENKINS -> ingestJenkins(root, platform.getDefaultTenantId());
        };
        eventRepository.saveAll(events);
        return new SyncResponse(UUID.randomUUID(), sourceSystem, SyncStatus.COMPLETED, events.size(), startedAt, Instant.now(),
                "Webhook ingested and normalized from " + sourceSystem);
    }

    private List<EngineeringEventEntity> ingestGit(SourceSystem sourceSystem, Map<String, String> headers, JsonNode root, String tenantId) {
        String eventName = header(headers, sourceSystem == SourceSystem.GITHUB ? "x-github-event" : "x-gitlab-event");
        JsonNode repositoryNode = root.has("repository") ? root.path("repository") : root.path("project");
        RepositoryEntity repository = upsertRepository(sourceSystem, repositoryNode, tenantId);
        if (eventName.toLowerCase(Locale.ROOT).contains("push") || root.has("commits")) {
            return ingestGitPush(sourceSystem, repository, root);
        }
        if (root.has("pull_request") || root.has("object_attributes")) {
            return ingestPullRequest(sourceSystem, repository, root);
        }
        if (root.has("release")) {
            return ingestRelease(sourceSystem, repository, root);
        }
        return List.of(event(sourceSystem, EngineeringEventType.SYNC_COMPLETED, "Repository", repository.getId().toString(),
                "Repository webhook received: " + repository.getName(), Instant.now(), repository.getTenantId()));
    }

    private List<EngineeringEventEntity> ingestGitPush(SourceSystem sourceSystem, RepositoryEntity repository, JsonNode root) {
        List<EngineeringEventEntity> events = new ArrayList<>();
        JsonNode commits = root.path("commits");
        if (commits.isArray()) {
            for (JsonNode commitNode : commits) {
                String hash = firstText(commitNode, "id", "sha");
                if (hash == null || hash.isBlank()) {
                    continue;
                }
                CommitMetadataEntity commit = commitRepository.findById(hash).orElseGet(CommitMetadataEntity::new);
                commit.setHash(hash);
                commit.setRepository(repository);
                commit.setMessage(text(commitNode, "message", ""));
                commit.setAuthorName(text(commitNode.path("author"), "name", null));
                commit.setAuthorEmail(text(commitNode.path("author"), "email", null));
                commit.setCommittedAt(parseInstant(firstText(commitNode, "timestamp", "date"), Instant.now()));
                commit.setChangedFiles(changedFiles(commitNode));
                commitRepository.save(commit);
                relationshipService.ensureRelationship(repository.getTenantId(), "Repository", repository.getId().toString(),
                        RelationshipType.REPOSITORY_CONTAINS_COMMIT, "Commit", commit.getHash());
                linkJiraKeys(repository.getTenantId(), "Commit", commit.getHash(), commit.getMessage());
                events.add(event(sourceSystem, EngineeringEventType.COMMIT_CREATED, "Commit", commit.getHash(),
                        "Commit created: " + truncate(commit.getMessage(), 120), commit.getCommittedAt(), repository.getTenantId()));
            }
        }
        return events;
    }

    private List<EngineeringEventEntity> ingestPullRequest(SourceSystem sourceSystem, RepositoryEntity repository, JsonNode root) {
        JsonNode prNode = root.has("pull_request") ? root.path("pull_request") : root.path("object_attributes");
        Integer number = intValue(firstText(prNode, "number", "iid", "id"), 0);
        PullRequestEntity pullRequest = pullRequestRepository.findByRepositoryIdAndNumber(repository.getId(), number).orElseGet(PullRequestEntity::new);
        pullRequest.setRepository(repository);
        pullRequest.setNumber(number);
        pullRequest.setTitle(text(prNode, "title", "Untitled pull request"));
        pullRequest.setStatus(firstText(prNode, "state", "action", "state_id"));
        pullRequest.setCreatedAt(parseInstant(firstText(prNode, "created_at"), Instant.now()));
        pullRequest.setMergedAt(parseInstant(firstText(prNode, "merged_at", "mergedAt"), null));
        pullRequestRepository.save(pullRequest);

        EngineeringEventType eventType = pullRequest.getMergedAt() != null || root.path("action").asText("").equals("closed") && prNode.path("merged").asBoolean(false)
                ? EngineeringEventType.PR_MERGED
                : EngineeringEventType.PR_CREATED;
        String prId = repository.getId() + "#" + pullRequest.getNumber();
        linkJiraKeys(repository.getTenantId(), "PullRequest", prId, pullRequest.getTitle());
        return List.of(event(sourceSystem, eventType, "PullRequest", prId,
                "Pull request event: " + pullRequest.getTitle(), eventType == EngineeringEventType.PR_MERGED ? Instant.now() : pullRequest.getCreatedAt(), repository.getTenantId()));
    }

    private List<EngineeringEventEntity> ingestRelease(SourceSystem sourceSystem, RepositoryEntity repository, JsonNode root) {
        JsonNode releaseNode = root.path("release");
        String version = firstText(releaseNode, "tag_name", "name");
        if (version == null) {
            version = "unknown-release";
        }
        ReleaseEntity release = releaseRepository.findByRepositoryIdAndVersion(repository.getId(), version).orElseGet(ReleaseEntity::new);
        release.setRepository(repository);
        release.setVersion(version);
        release.setTagName(firstText(releaseNode, "tag_name", "name"));
        release.setPublishedAt(parseInstant(firstText(releaseNode, "published_at", "released_at", "created_at"), Instant.now()));
        releaseRepository.save(release);
        return List.of(event(sourceSystem, EngineeringEventType.RELEASE_PUBLISHED, "Release", release.getVersion(),
                "Release published: " + release.getVersion(), release.getPublishedAt(), repository.getTenantId()));
    }

    private List<EngineeringEventEntity> ingestJira(JsonNode root, String tenantId) {
        JsonNode issueNode = root.path("issue");
        JsonNode fields = issueNode.path("fields");
        String key = text(issueNode, "key", firstText(root, "issue_key", "key"));
        if (key == null) {
            key = "UNKNOWN-" + UUID.randomUUID();
        }
        JiraIssueEntity issue = jiraIssueRepository.findById(key).orElseGet(JiraIssueEntity::new);
        issue.setIssueKey(key);
        issue.setTenantId(resolvedTenantId(tenantId));
        issue.setProjectKey(text(fields.path("project"), "key", projectFromIssueKey(key)));
        issue.setSummary(text(fields, "summary", text(root, "summary", "")));
        issue.setIssueType(text(fields.path("issuetype"), "name", text(root, "issueType", null)));
        issue.setStatus(text(fields.path("status"), "name", text(root, "status", null)));
        issue.setAssignee(text(fields.path("assignee"), "displayName", text(root, "assignee", null)));
        issue.setFixVersion(joinNames(fields.path("fixVersions")));
        issue.setLabels(joinTextArray(fields.path("labels")));
        issue.setComponents(joinNames(fields.path("components")));
        issue.setUpdatedAt(parseInstant(text(fields, "updated", text(root, "updated", null)), Instant.now()));
        jiraIssueRepository.save(issue);

        EngineeringEventType type = isClosed(issue.getStatus()) ? EngineeringEventType.STORY_CLOSED : EngineeringEventType.JIRA_ISSUE_UPDATED;
        return List.of(event(SourceSystem.JIRA, type, "JiraIssue", issue.getIssueKey(),
                "Jira issue updated: " + issue.getIssueKey(), issue.getUpdatedAt(), issue.getTenantId()));
    }

    private List<EngineeringEventEntity> ingestConfluence(JsonNode root, String tenantId) {
        JsonNode page = root.has("page") ? root.path("page") : root;
        String pageId = firstText(page, "id", "pageId", "contentId");
        if (pageId == null) {
            pageId = "unknown-page-" + UUID.randomUUID();
        }
        ConfluenceDocumentEntity document = confluenceDocumentRepository.findById(pageId).orElseGet(ConfluenceDocumentEntity::new);
        document.setPageId(pageId);
        document.setTenantId(resolvedTenantId(tenantId));
        document.setTitle(text(page, "title", text(page, "name", "Untitled Confluence document")));
        document.setDocumentType(detectDocumentType(document.getTitle(), firstText(page, "type")));
        document.setVersion(intValue(firstText(page.path("version"), "number"), 1));
        document.setAuthor(firstText(page.path("lastUpdated").path("by"), "displayName", "publicName"));
        document.setLastModifiedAt(parseInstant(firstText(page.path("version"), "when", "createdAt"), Instant.now()));
        document.setEmbeddingStatus("PENDING");
        confluenceDocumentRepository.save(document);
        return List.of(event(SourceSystem.CONFLUENCE, EngineeringEventType.ADR_UPDATED, "ConfluenceDocument", document.getPageId(),
                "Confluence document updated: " + document.getTitle(), document.getLastModifiedAt(), document.getTenantId()));
    }

    private List<EngineeringEventEntity> ingestJenkins(JsonNode root, String tenantId) {
        JsonNode build = root.has("build") ? root.path("build") : root;
        String buildNumber = firstText(build, "number", "buildNumber", "id");
        if (buildNumber == null) {
            buildNumber = "unknown-build-" + UUID.randomUUID();
        }
        String environment = firstText(build, "environment", "env");
        if (environment == null) {
            environment = "unknown";
        }
        DeploymentEntity deployment = deploymentRepository.findByBuildNumberAndEnvironment(buildNumber, environment).orElseGet(DeploymentEntity::new);
        deployment.setReleaseVersion(firstText(build, "releaseVersion", "release", "tag"));
        deployment.setEnvironment(environment);
        deployment.setBuildNumber(buildNumber);
        deployment.setBuildStatus(firstText(build, "status", "result"));
        deployment.setBuildDurationMs(longValue(firstText(build, "duration", "durationMs"), null));
        deployment.setCommitRange(firstText(build, "commitRange", "gitCommitRange", "commit"));
        deployment.setJiraVersions(firstText(build, "jiraVersions", "fixVersion"));
        deployment.setDeployedAt(parseInstant(firstText(build, "deployedAt", "timestamp", "finishedAt"), Instant.now()));
        deployment = deploymentRepository.save(deployment);
        return List.of(event(SourceSystem.JENKINS, EngineeringEventType.DEPLOYMENT_COMPLETED, "Deployment", deployment.getId().toString(),
                "Deployment completed: " + deployment.getEnvironment() + " " + deployment.getReleaseVersion(), deployment.getDeployedAt(), resolvedTenantId(tenantId)));
    }

    private RepositoryEntity upsertRepository(SourceSystem sourceSystem, JsonNode repositoryNode, String tenantId) {
        String resolvedTenantId = resolvedTenantId(tenantId);
        String externalId = firstText(repositoryNode, "full_name", "path_with_namespace", "name", "id");
        if (externalId == null) {
            externalId = "unknown";
        }
        String name = firstText(repositoryNode, "name", "full_name", "path_with_namespace");
        RepositoryEntity repository = repositoryRepository.findByTenantIdAndSourceSystemAndExternalId(resolvedTenantId, sourceSystem, externalId)
                .orElseGet(RepositoryEntity::new);
        repository.setTenantId(resolvedTenantId);
        repository.setSourceSystem(sourceSystem);
        repository.setExternalId(externalId);
        repository.setName(name == null ? externalId : name);
        repository.setDefaultBranch(firstText(repositoryNode, "default_branch", "defaultBranch"));
        repository.setUrl(firstText(repositoryNode, "html_url", "web_url", "url"));
        repository.setLastSyncedAt(Instant.now());
        return repositoryRepository.save(repository);
    }

    private void validateWebhookToken(SourceSystem sourceSystem, Map<String, String> headers, ExternalPlatformProperties.BasePlatform platform) {
        String expectedSecret = platform.getWebhookSecret();
        if (expectedSecret == null || expectedSecret.isBlank()) {
            return;
        }
        String headerName = platform.getWebhookTokenHeader();
        String actualSecret = header(headers, headerName == null || headerName.isBlank() ? "x-sentinelai-webhook-token" : headerName);
        if (!expectedSecret.equals(actualSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook token for " + sourceSystem);
        }
    }

    private ExternalPlatformProperties.BasePlatform platform(SourceSystem sourceSystem) {
        return switch (sourceSystem) {
            case GITHUB -> platformProperties.getGithub();
            case GITLAB -> platformProperties.getGitlab();
            case JIRA -> platformProperties.getJira();
            case CONFLUENCE -> platformProperties.getConfluence();
            case JENKINS -> platformProperties.getJenkins();
        };
    }

    private String resolvedTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    private void linkJiraKeys(String tenantId, String sourceType, String sourceId, String text) {
        if (text == null) {
            return;
        }
        Matcher matcher = JIRA_KEY_PATTERN.matcher(text);
        while (matcher.find()) {
            relationshipService.ensureRelationship(tenantId, sourceType, sourceId, RelationshipType.PULL_REQUEST_LINKS_JIRA, "JiraIssue", matcher.group());
        }
    }

    private EngineeringEventEntity event(SourceSystem source, EngineeringEventType type, String subjectType, String subjectId, String title, Instant occurredAt, String tenantId) {
        EngineeringEventEntity event = new EngineeringEventEntity();
        event.setTenantId(tenantId);
        event.setSourceSystem(source);
        event.setEventType(type);
        event.setSubjectType(subjectType);
        event.setSubjectId(subjectId == null ? "unknown" : subjectId);
        event.setTitle(title == null ? type.name() : title);
        event.setOccurredAt(occurredAt == null ? Instant.now() : occurredAt);
        return event;
    }

    private List<String> changedFiles(JsonNode commitNode) {
        Set<String> files = new HashSet<>();
        addArray(files, commitNode.path("added"));
        addArray(files, commitNode.path("modified"));
        addArray(files, commitNode.path("removed"));
        return new ArrayList<>(files);
    }

    private void addArray(Set<String> values, JsonNode array) {
        if (array.isArray()) {
            array.forEach(item -> values.add(item.asText()));
        }
    }

    private String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private String text(JsonNode node, String name, String defaultValue) {
        String value = firstText(node, name);
        return value == null ? defaultValue : value;
    }

    private Instant parseInstant(String value, Instant defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return defaultValue;
        }
    }

    private Integer intValue(String value, Integer defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private Long longValue(String value, Long defaultValue) {
        try {
            return value == null ? defaultValue : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String joinNames(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            array.forEach(item -> values.add(text(item, "name", "")));
        }
        return String.join(",", values);
    }

    private String joinTextArray(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) {
            array.forEach(item -> values.add(item.asText()));
        }
        return String.join(",", values);
    }

    private boolean isClosed(String status) {
        return status != null && List.of("done", "closed", "resolved").contains(status.toLowerCase(Locale.ROOT));
    }

    private String projectFromIssueKey(String key) {
        return key == null || !key.contains("-") ? "UNKNOWN" : key.substring(0, key.indexOf('-'));
    }

    private String detectDocumentType(String title, String fallback) {
        if (title != null) {
            String lower = title.toLowerCase(Locale.ROOT);
            if (lower.contains("adr")) {
                return "ADR";
            }
            if (lower.contains("runbook")) {
                return "RUNBOOK";
            }
            if (lower.contains("rfc")) {
                return "RFC";
            }
        }
        return fallback == null ? "TECHNICAL_DOCUMENTATION" : fallback.toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

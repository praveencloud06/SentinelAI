package com.sentinelai.knowledge.embedding;

import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ConfluenceDocumentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

/**
 * Builds human-readable engineering context strings from connector entity metadata.
 *
 * <p>The resulting text is passed to the embedding generation pipeline to produce
 * semantically rich vectors for AI-assisted Root Cause Analysis (RCA) and
 * semantic search. It is for internal processing only and does not alter any
 * REST API response.
 *
 * <p>Each {@code build*()} method is deliberately lenient: null or blank fields
 * are omitted so that partial metadata still produces a useful context string.
 */
@Component
public class EngineeringContextBuilder {

    // -------------------------------------------------------------------------
    // GitHub / Git
    // -------------------------------------------------------------------------

    /**
     * Builds engineering context for a single commit.
     *
     * <pre>
     * Repository: payment-service
     * Branch: main
     * Commit: PAY-221 add timeout guard for gateway retries
     * Author: SentinelAI Engineer
     * Modified Files:
     * - RetryPolicy.java
     * </pre>
     */
    public String buildCommitContext(CommitMetadataEntity commit) {
        StringBuilder sb = new StringBuilder();

        if (commit.getRepository() != null && hasText(commit.getRepository().getName())) {
            sb.append("Repository: ").append(commit.getRepository().getName()).append('\n');
        }

        if (hasText(commit.getBranch())) {
            sb.append("Branch: ").append(commit.getBranch()).append('\n');
        }

        if (hasText(commit.getMessage())) {
            sb.append("Commit: ").append(firstLine(commit.getMessage())).append('\n');
        }

        if (hasText(commit.getAuthorName())) {
            sb.append("Author: ").append(commit.getAuthorName()).append('\n');
        }

        List<String> files = commit.getChangedFiles();
        if (files != null && !files.isEmpty()) {
            sb.append("Modified Files:\n");
            files.forEach(f -> sb.append("- ").append(f).append('\n'));
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Jira
    // -------------------------------------------------------------------------

    /**
     * Builds engineering context for a Jira issue.
     *
     * <pre>
     * Issue: PAY-221
     * Summary: Payment gateway retry timeout failures
     * Description: Gateway retries exceeded timeout because …
     * Status: Done
     * Component: payment-service
     * Fix Version: v1.8.2
     * </pre>
     */
    public String buildJiraContext(JiraIssueEntity issue) {
        StringBuilder sb = new StringBuilder();

        if (hasText(issue.getIssueKey())) {
            sb.append("Issue: ").append(issue.getIssueKey()).append('\n');
        }

        if (hasText(issue.getSummary())) {
            sb.append("Summary: ").append(issue.getSummary()).append('\n');
        }

        if (hasText(issue.getDescription())) {
            sb.append("Description: ").append(issue.getDescription().trim()).append('\n');
        }

        if (hasText(issue.getStatus())) {
            sb.append("Status: ").append(issue.getStatus()).append('\n');
        }

        if (hasText(issue.getComponents())) {
            sb.append("Component: ").append(issue.getComponents()).append('\n');
        }

        if (hasText(issue.getFixVersion())) {
            sb.append("Fix Version: ").append(issue.getFixVersion()).append('\n');
        }

        if (hasText(issue.getAssignee())) {
            sb.append("Assignee: ").append(issue.getAssignee()).append('\n');
        }

        if (hasText(issue.getLabels())) {
            sb.append("Labels: ").append(issue.getLabels()).append('\n');
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Confluence
    // -------------------------------------------------------------------------

    /**
     * Builds engineering context for a Confluence page.
     *
     * <pre>
     * Document: ADR Payment Gateway Retry Policy
     *
     * Content:
     * The retry policy introduces exponential backoff…
     * </pre>
     */
    public String buildConfluenceContext(ConfluenceDocumentEntity document) {
        StringBuilder sb = new StringBuilder();

        if (hasText(document.getTitle())) {
            sb.append("Document: ").append(document.getTitle()).append('\n');
        }

        if (hasText(document.getDocumentType())) {
            sb.append("Type: ").append(document.getDocumentType()).append('\n');
        }

        if (hasText(document.getBody())) {
            sb.append('\n').append("Content:\n").append(document.getBody().trim()).append('\n');
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Jenkins
    // -------------------------------------------------------------------------

    /**
     * Builds engineering context for a Jenkins deployment.
     *
     * <pre>
     * Build: 145
     * Environment: staging
     * Version: v1.8.2
     * Status: SUCCESS
     * Commit Range: abc000..abc123
     * </pre>
     */
    public String buildDeploymentContext(DeploymentEntity deployment) {
        StringBuilder sb = new StringBuilder();

        if (hasText(deployment.getBuildNumber())) {
            sb.append("Build: ").append(deployment.getBuildNumber()).append('\n');
        }

        if (hasText(deployment.getEnvironment())) {
            sb.append("Environment: ").append(deployment.getEnvironment()).append('\n');
        }

        if (hasText(deployment.getReleaseVersion())) {
            sb.append("Version: ").append(deployment.getReleaseVersion()).append('\n');
        }

        if (hasText(deployment.getBuildStatus())) {
            sb.append("Status: ").append(deployment.getBuildStatus()).append('\n');
        }

        if (hasText(deployment.getCommitRange())) {
            sb.append("Commit Range: ").append(deployment.getCommitRange()).append('\n');
        }

        if (deployment.getBuildDurationMs() != null) {
            sb.append("Duration: ").append(deployment.getBuildDurationMs()).append("ms\n");
        }

        if (hasText(deployment.getJiraVersions())) {
            sb.append("Jira Versions: ").append(deployment.getJiraVersions()).append('\n');
        }

        if (deployment.getDeployedAt() != null) {
            sb.append("Deployed At: ").append(deployment.getDeployedAt()).append('\n');
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Returns the first non-blank line of a multi-line string, trimmed. */
    private String firstLine(String text) {
        if (text == null) {
            return "";
        }
        String[] lines = text.split("\\r?\\n", 2);
        return lines[0].trim();
    }
}

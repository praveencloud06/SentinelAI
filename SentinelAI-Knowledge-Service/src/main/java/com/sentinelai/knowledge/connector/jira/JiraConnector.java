package com.sentinelai.knowledge.connector.jira;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.connector.ConnectorSyncResult;
import com.sentinelai.knowledge.connector.EngineeringConnector;
import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import com.sentinelai.knowledge.infrastructure.repository.JiraIssueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JiraConnector implements EngineeringConnector {
    private final JiraIssueJpaRepository jiraIssueRepository;

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.JIRA;
    }

    @Override
    public ConnectorSyncResult sync(SyncRequest request) {
        String tenantId = request.resolvedTenantId();
        JiraIssueEntity issue = jiraIssueRepository.findById("PAY-101").orElseGet(JiraIssueEntity::new);
        issue.setIssueKey("PAY-101");
        issue.setTenantId(tenantId);
        issue.setProjectKey("PAY");
        issue.setSummary("Improve payment retry observability");
        issue.setIssueType("Story");
        issue.setStatus("Closed");
        issue.setAssignee("SentinelAI Engineering");
        issue.setFixVersion("v0.1.0");
        issue.setLabels("observability,payment");
        issue.setComponents("payment-service");
        issue.setUpdatedAt(Instant.now().minusSeconds(2400));
        jiraIssueRepository.save(issue);

        EngineeringEventEntity event = new EngineeringEventEntity();
        event.setTenantId(tenantId);
        event.setSourceSystem(SourceSystem.JIRA);
        event.setEventType(EngineeringEventType.STORY_CLOSED);
        event.setSubjectType("JiraIssue");
        event.setSubjectId(issue.getIssueKey());
        event.setTitle("Story closed: " + issue.getIssueKey());
        event.setOccurredAt(issue.getUpdatedAt());
        return new ConnectorSyncResult("Jira metadata synchronized using connector foundation", List.of(event));
    }
}

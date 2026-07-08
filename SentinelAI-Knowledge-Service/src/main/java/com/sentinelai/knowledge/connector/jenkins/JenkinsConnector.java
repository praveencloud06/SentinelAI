package com.sentinelai.knowledge.connector.jenkins;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.connector.ConnectorSyncResult;
import com.sentinelai.knowledge.connector.EngineeringConnector;
import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.repository.DeploymentJpaRepository;
import com.sentinelai.knowledge.infrastructure.repository.RepositoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JenkinsConnector implements EngineeringConnector {
    private final DeploymentJpaRepository deploymentRepository;
    private final RepositoryJpaRepository repositoryRepository;

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.JENKINS;
    }

    @Override
    public ConnectorSyncResult sync(SyncRequest request) {
        DeploymentEntity deployment = deploymentRepository.findByBuildNumberAndEnvironment("145", "staging").orElseGet(DeploymentEntity::new);
        deployment.setRepository(repositoryRepository.findAll().stream().findFirst().orElse(null));
        deployment.setReleaseVersion("v0.1.0");
        deployment.setEnvironment("staging");
        deployment.setBuildNumber("145");
        deployment.setBuildStatus("SUCCESS");
        deployment.setBuildDurationMs(182000L);
        deployment.setCommitRange("9f3a1c7-sample..HEAD");
        deployment.setJiraVersions("v0.1.0");
        deployment.setDeployedAt(Instant.now().minusSeconds(900));
        deployment = deploymentRepository.save(deployment);

        EngineeringEventEntity event = new EngineeringEventEntity();
        event.setTenantId(request.resolvedTenantId());
        event.setSourceSystem(SourceSystem.JENKINS);
        event.setEventType(EngineeringEventType.DEPLOYMENT_COMPLETED);
        event.setSubjectType("Deployment");
        event.setSubjectId(deployment.getId().toString());
        event.setTitle("Deployment completed: " + deployment.getEnvironment() + " " + deployment.getReleaseVersion());
        event.setOccurredAt(deployment.getDeployedAt());
        return new ConnectorSyncResult("Jenkins metadata synchronized using connector foundation", List.of(event));
    }
}

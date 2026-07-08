package com.sentinelai.knowledge.api.controller;

import com.sentinelai.knowledge.api.dto.CommitResponse;
import com.sentinelai.knowledge.api.dto.DeploymentResponse;
import com.sentinelai.knowledge.api.dto.JiraIssueResponse;
import com.sentinelai.knowledge.api.dto.RelationshipResponse;
import com.sentinelai.knowledge.api.dto.ReleaseResponse;
import com.sentinelai.knowledge.api.dto.RepositoryResponse;
import com.sentinelai.knowledge.api.dto.TimelineEventResponse;
import com.sentinelai.knowledge.api.mapper.KnowledgeMapper;
import com.sentinelai.knowledge.application.KnowledgeQueryService;
import com.sentinelai.knowledge.application.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeQueryService queryService;
    private final RelationshipService relationshipService;

    @GetMapping("/repositories")
    public List<RepositoryResponse> repositories() {
        return queryService.repositories().stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/repositories/{id}")
    public RepositoryResponse repository(@PathVariable UUID id) {
        return KnowledgeMapper.toResponse(queryService.repository(id));
    }

    @GetMapping("/commits")
    public List<CommitResponse> commits(@RequestParam(required = false) UUID repositoryId) {
        return queryService.commits(repositoryId).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/commits/{hash}")
    public CommitResponse commit(@PathVariable String hash) {
        return KnowledgeMapper.toResponse(queryService.commit(hash));
    }

    @GetMapping("/jira/issues")
    public List<JiraIssueResponse> jiraIssues(@RequestParam(required = false) String projectKey,
                                              @RequestParam(required = false) String fixVersion) {
        return queryService.jiraIssues(projectKey, fixVersion).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/deployments")
    public List<DeploymentResponse> deployments(@RequestParam(required = false) String releaseVersion,
                                                @RequestParam(required = false) String commitHash) {
        return queryService.deployments(releaseVersion, commitHash).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/releases")
    public List<ReleaseResponse> releases(@RequestParam(required = false) String version) {
        return queryService.releases(version).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/timeline")
    public List<TimelineEventResponse> timeline(@RequestParam(required = false) String tenantId,
                                                @RequestParam(defaultValue = "100") int limit) {
        return queryService.timeline(tenantId, limit).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/relationships")
    public List<RelationshipResponse> relationships(@RequestParam(required = false) String sourceType,
                                                    @RequestParam(required = false) String sourceId,
                                                    @RequestParam(required = false) String targetType,
                                                    @RequestParam(required = false) String targetId) {
        return relationshipService.find(sourceType, sourceId, targetType, targetId).stream().map(KnowledgeMapper::toResponse).toList();
    }
}

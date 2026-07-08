package com.sentinelai.knowledge.api.controller;

import com.sentinelai.knowledge.api.dto.DeploymentResponse;
import com.sentinelai.knowledge.api.dto.JiraIssueResponse;
import com.sentinelai.knowledge.api.dto.RelationshipResponse;
import com.sentinelai.knowledge.api.dto.ReleaseResponse;
import com.sentinelai.knowledge.api.mapper.KnowledgeMapper;
import com.sentinelai.knowledge.application.KnowledgeQueryService;
import com.sentinelai.knowledge.application.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final KnowledgeQueryService queryService;
    private final RelationshipService relationshipService;

    @GetMapping("/deployments")
    public List<DeploymentResponse> deploymentsForCommit(@RequestParam String commitHash) {
        return queryService.deployments(null, commitHash).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/jira")
    public List<RelationshipResponse> jiraForCommit(@RequestParam String commitHash) {
        return relationshipService.find("Commit", commitHash, null, null).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/releases")
    public List<ReleaseResponse> releaseForDeployment(@RequestParam String version) {
        return queryService.releases(version).stream().map(KnowledgeMapper::toResponse).toList();
    }

    @GetMapping("/changes-between-releases")
    public List<JiraIssueResponse> changesBetweenReleases(@RequestParam String fromVersion,
                                                          @RequestParam String toVersion) {
        return queryService.jiraIssues(null, toVersion).stream().map(KnowledgeMapper::toResponse).toList();
    }
}

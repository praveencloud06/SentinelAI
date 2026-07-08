package com.sentinelai.knowledge.api.controller;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.api.dto.SyncResponse;
import com.sentinelai.knowledge.application.SyncService;
import com.sentinelai.knowledge.domain.SourceSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {
    private final SyncService syncService;

    @PostMapping("/github")
    public SyncResponse syncGithub(@RequestBody(required = false) SyncRequest request) {
        return syncService.sync(SourceSystem.GITHUB, request);
    }

    @PostMapping("/jira")
    public SyncResponse syncJira(@RequestBody(required = false) SyncRequest request) {
        return syncService.sync(SourceSystem.JIRA, request);
    }

    @PostMapping("/confluence")
    public SyncResponse syncConfluence(@RequestBody(required = false) SyncRequest request) {
        return syncService.sync(SourceSystem.CONFLUENCE, request);
    }

    @PostMapping("/jenkins")
    public SyncResponse syncJenkins(@RequestBody(required = false) SyncRequest request) {
        return syncService.sync(SourceSystem.JENKINS, request);
    }

    @PostMapping("/all")
    public List<SyncResponse> syncAll(@RequestBody(required = false) SyncRequest request) {
        return syncService.syncAll(request == null ? new SyncRequest(null, null, null) : request);
    }
}

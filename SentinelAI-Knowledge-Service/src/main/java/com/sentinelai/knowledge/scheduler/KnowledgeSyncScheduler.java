package com.sentinelai.knowledge.scheduler;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.application.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KnowledgeSyncScheduler {
    private final SyncService syncService;

    @Value("${sentinelai.knowledge.sync.scheduled-enabled:false}")
    private boolean scheduledEnabled;

    @Scheduled(fixedDelayString = "${sentinelai.knowledge.sync.fixed-delay-ms:900000}")
    public void scheduledSync() {
        if (scheduledEnabled) {
            syncService.syncAll(new SyncRequest(null, null, null));
        }
    }
}

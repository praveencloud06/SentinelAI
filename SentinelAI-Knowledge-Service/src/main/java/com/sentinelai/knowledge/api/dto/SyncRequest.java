package com.sentinelai.knowledge.api.dto;

import com.sentinelai.knowledge.domain.SyncMode;

import java.util.Map;

public record SyncRequest(
        String tenantId,
        SyncMode mode,
        Map<String, String> parameters
) {
    public String resolvedTenantId() {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    public SyncMode resolvedMode() {
        return mode == null ? SyncMode.INCREMENTAL : mode;
    }
}

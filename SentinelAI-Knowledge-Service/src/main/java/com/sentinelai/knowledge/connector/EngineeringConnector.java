package com.sentinelai.knowledge.connector;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.domain.SourceSystem;

public interface EngineeringConnector {
    SourceSystem sourceSystem();
    ConnectorSyncResult sync(SyncRequest request);
}

package com.sentinelai.knowledge.application;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.api.dto.SyncResponse;
import com.sentinelai.knowledge.connector.ConnectorSyncResult;
import com.sentinelai.knowledge.connector.EngineeringConnector;
import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.domain.SyncStatus;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.repository.EngineeringEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SyncService {
    private final List<EngineeringConnector> connectors;
    private final EngineeringEventJpaRepository eventRepository;

    @Transactional
    public SyncResponse sync(SourceSystem sourceSystem, SyncRequest request) {
        Instant startedAt = Instant.now();
        EngineeringConnector connector = connectorMap().get(sourceSystem);
        if (connector == null) {
            return new SyncResponse(UUID.randomUUID(), sourceSystem, SyncStatus.FAILED, 0, startedAt, Instant.now(), "Connector is not registered");
        }

        ConnectorSyncResult result = connector.sync(request == null ? new SyncRequest(null, null, null) : request);
        List<EngineeringEventEntity> savedEvents = eventRepository.saveAll(result.events());
        return new SyncResponse(UUID.randomUUID(), sourceSystem, SyncStatus.COMPLETED, savedEvents.size(), startedAt, Instant.now(), result.message());
    }

    @Transactional
    public List<SyncResponse> syncAll(SyncRequest request) {
        return connectorMap().keySet().stream()
                .map(sourceSystem -> sync(sourceSystem, request))
                .toList();
    }

    private Map<SourceSystem, EngineeringConnector> connectorMap() {
        Map<SourceSystem, EngineeringConnector> bySource = new EnumMap<>(SourceSystem.class);
        connectors.forEach(connector -> bySource.put(connector.sourceSystem(), connector));
        return bySource;
    }
}

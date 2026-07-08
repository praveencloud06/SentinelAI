package com.sentinelai.knowledge.connector.confluence;

import com.sentinelai.knowledge.api.dto.SyncRequest;
import com.sentinelai.knowledge.connector.ConnectorSyncResult;
import com.sentinelai.knowledge.connector.EngineeringConnector;
import com.sentinelai.knowledge.domain.EngineeringEventType;
import com.sentinelai.knowledge.domain.SourceSystem;
import com.sentinelai.knowledge.infrastructure.persistence.ConfluenceDocumentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.EngineeringEventEntity;
import com.sentinelai.knowledge.infrastructure.repository.ConfluenceDocumentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfluenceConnector implements EngineeringConnector {
    private final ConfluenceDocumentJpaRepository documentRepository;

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.CONFLUENCE;
    }

    @Override
    public ConnectorSyncResult sync(SyncRequest request) {
        String tenantId = request.resolvedTenantId();
        ConfluenceDocumentEntity document = documentRepository.findById("ARCH-001").orElseGet(ConfluenceDocumentEntity::new);
        document.setPageId("ARCH-001");
        document.setTenantId(tenantId);
        document.setTitle("Payment Service Retry Architecture");
        document.setDocumentType("ADR");
        document.setVersion(3);
        document.setAuthor("SentinelAI Engineering");
        document.setLastModifiedAt(Instant.now().minusSeconds(1800));
        document.setEmbeddingStatus("PENDING");
        documentRepository.save(document);

        EngineeringEventEntity event = new EngineeringEventEntity();
        event.setTenantId(tenantId);
        event.setSourceSystem(SourceSystem.CONFLUENCE);
        event.setEventType(EngineeringEventType.ADR_UPDATED);
        event.setSubjectType("ConfluenceDocument");
        event.setSubjectId(document.getPageId());
        event.setTitle("ADR updated: " + document.getTitle());
        event.setOccurredAt(document.getLastModifiedAt());
        return new ConnectorSyncResult("Confluence metadata synchronized using connector foundation", List.of(event));
    }
}

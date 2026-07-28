package com.sentinelai.knowledge.infrastructure.persistence;

import com.sentinelai.knowledge.domain.EmbeddingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "confluence_documents")
public class ConfluenceDocumentEntity {
    @Id
    @Column(name = "page_id", length = 120)
    private String pageId;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId = "default";

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(length = 80)
    private String documentType;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 200)
    private String author;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", length = 40)
    private EmbeddingStatus embeddingStatus;
}

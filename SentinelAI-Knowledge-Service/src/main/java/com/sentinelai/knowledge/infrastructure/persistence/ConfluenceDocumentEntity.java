package com.sentinelai.knowledge.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(length = 80)
    private String documentType;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 200)
    private String author;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @Column(name = "embedding_status", length = 80)
    private String embeddingStatus = "PENDING";
}

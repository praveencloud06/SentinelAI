package com.sentinelai.knowledge.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "commit_metadata")
public class CommitMetadataEntity {
    @Id
    @Column(name = "hash", length = 80)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryEntity repository;

    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(name = "author_email", length = 320)
    private String authorEmail;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt;

    @ElementCollection
    @CollectionTable(name = "commit_changed_files", joinColumns = @JoinColumn(name = "commit_hash"))
    @Column(name = "file_path", nullable = false, length = 1024)
    private List<String> changedFiles = new ArrayList<>();
}

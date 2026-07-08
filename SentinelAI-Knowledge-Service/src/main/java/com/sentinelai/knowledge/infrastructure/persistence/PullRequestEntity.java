package com.sentinelai.knowledge.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pull_requests", uniqueConstraints = @UniqueConstraint(name = "uk_pull_request_repo_number", columnNames = {"repository_id", "number"}))
public class PullRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryEntity repository;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(length = 80)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "merged_at")
    private Instant mergedAt;
}

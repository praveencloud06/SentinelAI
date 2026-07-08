package com.sentinelai.knowledge.infrastructure.repository;

import com.sentinelai.knowledge.infrastructure.persistence.ConfluenceDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfluenceDocumentJpaRepository extends JpaRepository<ConfluenceDocumentEntity, String> {
    List<ConfluenceDocumentEntity> findByDocumentTypeIgnoreCase(String documentType);
}

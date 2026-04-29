package com.sentinel.ai.repository;

import com.sentinel.ai.model.KnowledgeBaseEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntry, UUID> {
}

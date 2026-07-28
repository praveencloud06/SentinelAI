package com.sentinelai.knowledge.embedding;

import com.sentinelai.knowledge.chunking.DocumentChunker;
import com.sentinelai.knowledge.domain.EmbeddingGenerationException;
import com.sentinelai.knowledge.domain.EmbeddingProvider;
import com.sentinelai.knowledge.domain.EmbeddingStatus;
import com.sentinelai.knowledge.infrastructure.persistence.CommitMetadataEntity;
import com.sentinelai.knowledge.infrastructure.persistence.ConfluenceDocumentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.DeploymentEntity;
import com.sentinelai.knowledge.infrastructure.persistence.JiraIssueEntity;
import com.sentinelai.knowledge.infrastructure.persistence.KnowledgeEmbeddingEntity;
import com.sentinelai.knowledge.infrastructure.repository.KnowledgeEmbeddingJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Service for generating embeddings asynchronously.
 * Handles chunking, deduplication, and retry logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingGenerationService {
    
    private final EmbeddingProvider embeddingProvider;
    private final KnowledgeEmbeddingJpaRepository repository;
    private final DocumentChunker documentChunker;
    private final EngineeringContextBuilder contextBuilder;
    
    @Value("${sentinelai.knowledge.embedding.model:text-embedding-3-small}")
    private String embeddingModel;
    
    @Value("${sentinelai.knowledge.embedding.version:v1}")
    private String embeddingVersion;
    
    @Value("${sentinelai.knowledge.embedding.chunking.enabled:true}")
    private boolean chunkingEnabled;
    
    @Value("${sentinelai.knowledge.embedding.chunking.max-chunk-size:2000}")
    private int maxChunkSize;
    
    /**
     * Generate embedding for a single entity asynchronously.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateEmbedding(String entityType, String entityId, String content) {
        try {
            log.debug("Starting embedding generation for {}:{} in thread: {}", 
                    entityType, entityId, Thread.currentThread().getName());
            
            // Check if embedding already exists
            List<KnowledgeEmbeddingEntity> existing = repository.findByEntityTypeAndEntityId(entityType, entityId);
            if (!existing.isEmpty() && allCompleted(existing)) {
                log.debug("Embedding already exists and completed for {}:{}", entityType, entityId);
                return;
            }
            
            // Generate content hash for deduplication
            String contentHash = generateContentHash(content);
            
            // Check for duplicate content
            repository.findByContentHash(contentHash).ifPresent(duplicate -> {
                log.debug("Found duplicate content, reusing embedding from {}", duplicate.getId());
                // Could implement deduplication logic here
            });
            
            // Chunk content if enabled and large
            List<String> chunks = chunkingEnabled && content.length() > maxChunkSize 
                    ? documentChunker.chunk(content) 
                    : List.of(content);
            
            // Generate embeddings for each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                generateChunkEmbedding(entityType, entityId, i, chunk, contentHash);
            }
            
            log.info("Successfully generated {} embeddings for {}:{}", chunks.size(), entityType, entityId);
            
        } catch (EmbeddingGenerationException e) {
            log.error("Failed to generate embedding for {}:{} (retryable: {})", 
                    entityType, entityId, e.isRetryable(), e);
            handleFailure(entityType, entityId, e.isRetryable());
        } catch (Exception e) {
            log.error("Unexpected error generating embedding for {}:{}", entityType, entityId, e);
            handleFailure(entityType, entityId, true);
        }
    }
    
    /**
     * Generate embedding for a single chunk.
     */
    private void generateChunkEmbedding(String entityType, String entityId, int chunkNumber, 
                                         String content, String contentHash) {
        try {
            // Update status to PROCESSING
            KnowledgeEmbeddingEntity entity = repository.findByEntityTypeAndEntityId(entityType, entityId)
                    .stream()
                    .filter(e -> e.getChunkNumber().equals(chunkNumber))
                    .findFirst()
                    .orElseGet(() -> {
                        KnowledgeEmbeddingEntity newEntity = new KnowledgeEmbeddingEntity();
                        newEntity.setEntityType(entityType);
                        newEntity.setEntityId(entityId);
                        newEntity.setChunkNumber(chunkNumber);
                        newEntity.setContent(content);
                        newEntity.setContentHash(contentHash);
                        newEntity.setEmbeddingModel(embeddingModel);
                        newEntity.setEmbeddingVersion(embeddingVersion);
                        newEntity.setStatus(EmbeddingStatus.PROCESSING);
                        return repository.save(newEntity);
                    });
            
            // Generate embedding vector
            List<Float> embeddingVector = embeddingProvider.generateEmbedding(content);
            
            // Store embedding
            // Convert List<Float> to float[] for pgvector storage
            float[] floatArray = new float[embeddingVector.size()];
            for (int j = 0; j < embeddingVector.size(); j++) {
                floatArray[j] = embeddingVector.get(j);
            }
            entity.setEmbeddingFromArray(floatArray);
            entity.setStatus(EmbeddingStatus.COMPLETED);
            entity.setLastEmbedded(Instant.now());
            repository.save(entity);
            
            log.debug("Generated embedding for chunk {} of {}:{}", chunkNumber, entityType, entityId);
            
        } catch (EmbeddingGenerationException e) {
            log.error("Failed to generate embedding for chunk {} of {}:{} (retryable: {})", 
                    chunkNumber, entityType, entityId, e.isRetryable(), e);
            handleChunkFailure(entityType, entityId, chunkNumber, e.isRetryable());
        }
    }
    
    /**
     * Handle embedding generation failure.
     */
    private void handleFailure(String entityType, String entityId, boolean retryable) {
        List<KnowledgeEmbeddingEntity> entities = repository.findByEntityTypeAndEntityId(entityType, entityId);
        entities.forEach(entity -> {
            entity.setStatus(retryable ? EmbeddingStatus.FAILED : EmbeddingStatus.SKIPPED);
            entity.setLastEmbedded(Instant.now());
        });
        repository.saveAll(entities);
    }
    
    /**
     * Handle chunk embedding generation failure.
     */
    private void handleChunkFailure(String entityType, String entityId, int chunkNumber, boolean retryable) {
        repository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .filter(e -> e.getChunkNumber().equals(chunkNumber))
                .findFirst()
                .ifPresent(entity -> {
                    entity.setStatus(retryable ? EmbeddingStatus.FAILED : EmbeddingStatus.SKIPPED);
                    entity.setLastEmbedded(Instant.now());
                    repository.save(entity);
                });
    }
    
    /**
     * Check if all embeddings are completed.
     */
    private boolean allCompleted(List<KnowledgeEmbeddingEntity> entities) {
        return entities.stream().allMatch(e -> e.getStatus() == EmbeddingStatus.COMPLETED);
    }
    
    /**
     * Generate SHA-256 hash of content for deduplication.
     */
    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 not available, using simple hash", e);
            return String.valueOf(content.hashCode());
        }
    }

    // -------------------------------------------------------------------------
    // Typed entity overloads — build engineering context then embed
    // -------------------------------------------------------------------------

    /**
     * Generates an embedding for a GitHub commit using the enriched engineering
     * context string (repository, branch, message, author, modified files).
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateEmbeddingForCommit(CommitMetadataEntity commit) {
        String context = contextBuilder.buildCommitContext(commit);
        if (context.isBlank()) {
            log.debug("Skipping embedding for commit {} — no context produced", commit.getHash());
            return;
        }
        log.debug("Generating commit embedding for hash={} branch={}", commit.getHash(), commit.getBranch());
        generateEmbedding("Commit", commit.getHash(), context);
    }

    /**
     * Generates an embedding for a Jira issue using the enriched engineering
     * context string (key, summary, description, status, component, fix version).
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateEmbeddingForJiraIssue(JiraIssueEntity issue) {
        String context = contextBuilder.buildJiraContext(issue);
        if (context.isBlank()) {
            log.debug("Skipping embedding for Jira issue {} — no context produced", issue.getIssueKey());
            return;
        }
        log.debug("Generating Jira embedding for issue={}", issue.getIssueKey());
        generateEmbedding("JiraIssue", issue.getIssueKey(), context);
    }

    /**
     * Generates an embedding for a Confluence page using the enriched engineering
     * context string (title, document type, body content).
     * Only pages belonging to configured engineering spaces should be passed here.
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateEmbeddingForConfluencePage(ConfluenceDocumentEntity document) {
        String context = contextBuilder.buildConfluenceContext(document);
        if (context.isBlank()) {
            log.debug("Skipping embedding for Confluence page {} — no context produced", document.getPageId());
            return;
        }
        log.debug("Generating Confluence embedding for pageId={} title={}", document.getPageId(), document.getTitle());
        generateEmbedding("ConfluenceDocument", document.getPageId(), context);
    }

    /**
     * Generates an embedding for a Jenkins deployment using the engineering
     * context string (build number, environment, version, status, commit range).
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateEmbeddingForDeployment(DeploymentEntity deployment) {
        String context = contextBuilder.buildDeploymentContext(deployment);
        if (context.isBlank()) {
            log.debug("Skipping embedding for deployment {} — no context produced",
                    deployment.getBuildNumber());
            return;
        }
        log.debug("Generating deployment embedding for build={} env={}",
                deployment.getBuildNumber(), deployment.getEnvironment());
        generateEmbedding("Deployment", deployment.getId() != null ? deployment.getId().toString() : deployment.getBuildNumber(), context);
    }

    /**
     * Check if embedding provider is available.
     */
    public boolean isProviderAvailable() {
        return embeddingProvider.isAvailable();
    }
    
    /**
     * Get current embedding model information.
     */
    public String getModelInfo() {
        return String.format("%s (provider: %s, dimension: %d)", 
                embeddingProvider.getModel(), 
                embeddingProvider.getName(), 
                embeddingProvider.getDimension());
    }
}
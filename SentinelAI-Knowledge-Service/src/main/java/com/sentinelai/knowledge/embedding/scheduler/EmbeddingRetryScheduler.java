package com.sentinelai.knowledge.embedding.scheduler;

import com.sentinelai.knowledge.domain.EmbeddingStatus;
import com.sentinelai.knowledge.infrastructure.persistence.KnowledgeEmbeddingEntity;
import com.sentinelai.knowledge.infrastructure.repository.KnowledgeEmbeddingJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler for retrying failed embedding generations.
 * Runs periodically to process failed embeddings with exponential backoff.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingRetryScheduler {
    
    private final KnowledgeEmbeddingJpaRepository repository;
    
    @Value("${sentinelai.knowledge.embedding.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${sentinelai.knowledge.embedding.retry.backoff-ms:5000}")
    private long backoffMs;
    
    @Value("${sentinelai.knowledge.embedding.retry.batch-size:10}")
    private int batchSize;
    
    /**
     * Retry failed embeddings every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${sentinelai.knowledge.embedding.retry.interval-ms:300000}")
    @Transactional
    public void retryFailedEmbeddings() {
        log.debug("Checking for failed embeddings to retry");
        
        Instant threshold = Instant.now().minusMillis(backoffMs);
        List<KnowledgeEmbeddingEntity> failedEmbeddings = 
                repository.findFailedEmbeddingsForRetry(threshold, PageRequest.of(0, batchSize));
        
        if (failedEmbeddings.isEmpty()) {
            log.debug("No failed embeddings to retry");
            return;
        }
        
        log.info("Found {} failed embeddings to retry", failedEmbeddings.size());
        
        // Reset status to PENDING for retry
        failedEmbeddings.forEach(entity -> {
            entity.setStatus(EmbeddingStatus.PENDING);
            entity.setLastEmbedded(null);
        });
        
        repository.saveAll(failedEmbeddings);
        log.info("Reset {} failed embeddings to PENDING for retry", failedEmbeddings.size());
    }
    
    /**
     * Clean up old permanently failed embeddings daily.
     */
    @Scheduled(cron = "${sentinelai.knowledge.embedding.cleanup.cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldFailedEmbeddings() {
        log.info("Cleaning up old failed embeddings");
        
        Instant threshold = Instant.now().minus(java.time.Duration.ofDays(30));
        int deleted = repository.deleteOldFailedEmbeddings(threshold);
        
        log.info("Deleted {} old failed embeddings older than {}", deleted, threshold);
    }
    
    /**
     * Monitor embedding generation backlog.
     */
    @Scheduled(fixedDelayString = "${sentinelai.knowledge.embedding.monitor.interval-ms:60000}")
    public void monitorEmbeddingBacklog() {
        long pendingCount = repository.countByStatus(EmbeddingStatus.PENDING);
        long processingCount = repository.countByStatus(EmbeddingStatus.PROCESSING);
        long failedCount = repository.countByStatus(EmbeddingStatus.FAILED);
        long completedCount = repository.countByStatus(EmbeddingStatus.COMPLETED);
        
        log.info("Embedding status - Pending: {}, Processing: {}, Failed: {}, Completed: {}", 
                pendingCount, processingCount, failedCount, completedCount);
        
        // Alert if backlog is too high
        if (pendingCount > 1000) {
            log.warn("High embedding backlog: {} pending embeddings", pendingCount);
        }
        
        // Alert if failure rate is too high
        long total = pendingCount + processingCount + failedCount + completedCount;
        if (total > 0) {
            double failureRate = (double) failedCount / total;
            if (failureRate > 0.1) { // 10% failure rate
                log.warn("High embedding failure rate: {}%", (failureRate * 100));
            }
        }
    }
}
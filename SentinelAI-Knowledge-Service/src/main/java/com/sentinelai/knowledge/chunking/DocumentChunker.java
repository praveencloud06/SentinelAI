package com.sentinelai.knowledge.chunking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for chunking large documents into smaller pieces for embedding generation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentChunker {
    
    private final ChunkingStrategy chunkingStrategy;
    
    @Value("${sentinelai.knowledge.embedding.chunking.max-chunk-size:2000}")
    private int maxChunkSize;
    
    @Value("${sentinelai.knowledge.embedding.chunking.chunk-overlap:200}")
    private int chunkOverlap;
    
    /**
     * Chunk a document into smaller pieces for embedding.
     */
    public List<String> chunk(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        
        if (content.length() <= maxChunkSize) {
            return List.of(content);
        }
        
        log.debug("Chunking document of size {} into chunks of max {} characters", 
                content.length(), maxChunkSize);
        
        return chunkingStrategy.chunk(content, maxChunkSize, chunkOverlap);
    }
    
    /**
     * Estimate number of chunks for a document.
     */
    public int estimateChunkCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        if (content.length() <= maxChunkSize) {
            return 1;
        }
        return (int) Math.ceil((double) content.length() / (maxChunkSize - chunkOverlap));
    }
}
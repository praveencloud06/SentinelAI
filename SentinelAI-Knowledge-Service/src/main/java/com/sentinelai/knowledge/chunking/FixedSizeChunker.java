package com.sentinelai.knowledge.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-size chunking strategy that splits content at exact character boundaries.
 * Simple and predictable but may break at unnatural points.
 */
@Slf4j
@Component
public class FixedSizeChunker implements ChunkingStrategy {
    
    @Override
    public List<String> chunk(String content, int maxChunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return chunks;
        }
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxChunkSize, content.length());
            
            // Try to break at whitespace if possible
            if (end < content.length()) {
                int lastSpace = content.lastIndexOf(' ', end);
                if (lastSpace > start + maxChunkSize / 2) {
                    end = lastSpace;
                }
            }
            
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            start = end - overlap;
            if (start < 0) {
                start = end;
            }
        }
        
        log.debug("Chunked content into {} chunks using fixed-size strategy", chunks.size());
        return chunks;
    }
}
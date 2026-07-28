package com.sentinelai.knowledge.chunking;

import java.util.List;

/**
 * Strategy interface for document chunking algorithms.
 */
public interface ChunkingStrategy {
    
    /**
     * Chunk a document into smaller pieces.
     * 
     * @param content the full document content
     * @param maxChunkSize maximum size of each chunk
     * @param overlap number of characters to overlap between chunks
     * @return list of chunked content pieces
     */
    List<String> chunk(String content, int maxChunkSize, int overlap);
}
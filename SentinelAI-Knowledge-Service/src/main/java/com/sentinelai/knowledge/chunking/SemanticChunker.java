package com.sentinelai.knowledge.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic chunking strategy that breaks content at natural boundaries (sentences, paragraphs).
 * Maintains context by breaking at meaningful points rather than arbitrary character limits.
 * 
 * This is the PRIMARY (default) chunking strategy. Use FixedSizeChunker as an alternative.
 */
@Slf4j
@Component
@org.springframework.context.annotation.Primary
public class SemanticChunker implements ChunkingStrategy {
    
    @Override
    public List<String> chunk(String content, int maxChunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        // Split into paragraphs first
        String[] paragraphs = content.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                continue;
            }
            
            // If adding this paragraph would exceed the limit
            if (currentChunk.length() + trimmedParagraph.length() > maxChunkSize) {
                // Save current chunk if not empty
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                
                // Start new chunk with overlap if possible
                if (overlap > 0 && currentChunk.length() > overlap) {
                    String overlapText = currentChunk.substring(
                            Math.max(0, currentChunk.length() - overlap)
                    ).trim();
                    currentChunk = new StringBuilder(overlapText);
                    if (!overlapText.isEmpty()) {
                        currentChunk.append("\n\n");
                    }
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            
            // Add paragraph to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmedParagraph);
        }
        
        // Add final chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        // Handle very long paragraphs that still exceed the limit
        return handleLongChunks(chunks, maxChunkSize, overlap);
    }
    
    /**
     * Handle chunks that are still too long by splitting at sentence boundaries.
     */
    private List<String> handleLongChunks(List<String> chunks, int maxChunkSize, int overlap) {
        List<String> finalChunks = new ArrayList<>();
        
        for (String chunk : chunks) {
            if (chunk.length() <= maxChunkSize) {
                finalChunks.add(chunk);
            } else {
                // Split at sentence boundaries
                finalChunks.addAll(splitAtSentences(chunk, maxChunkSize, overlap));
            }
        }
        
        return finalChunks;
    }
    
    /**
     * Split text at sentence boundaries.
     */
    private List<String> splitAtSentences(String text, int maxChunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        // Simple sentence splitting (can be enhanced with NLP libraries)
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                
                // Add overlap
                if (overlap > 0 && currentChunk.length() > overlap) {
                    String overlapText = currentChunk.substring(
                            Math.max(0, currentChunk.length() - overlap)
                    ).trim();
                    currentChunk = new StringBuilder(overlapText);
                    if (!overlapText.isEmpty()) {
                        currentChunk.append(" ");
                    }
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}
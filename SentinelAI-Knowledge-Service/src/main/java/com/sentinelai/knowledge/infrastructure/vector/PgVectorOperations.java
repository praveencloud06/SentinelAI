package com.sentinelai.knowledge.infrastructure.vector;

import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for pgvector operations.
 * Handles conversion between Java arrays and pgvector format.
 */
@Slf4j
@Component
public class PgVectorOperations {
    
    /**
     * Convert float array to pgvector format string.
     * pgvector expects format: "[0.1,0.2,0.3,...]"
     */
    public String arrayToVector(float[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    /**
     * Convert pgvector string to float array.
     */
    public float[] vectorToArray(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return new float[0];
        }
        
        // Remove brackets and split by comma
        String cleaned = vectorString.replaceAll("[\\[\\]]", "").trim();
        if (cleaned.isEmpty()) {
            return new float[0];
        }
        
        String[] parts = cleaned.split(",");
        float[] array = new float[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            try {
                array[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse vector component: {}", parts[i]);
                array[i] = 0.0f;
            }
        }
        
        return array;
    }
    
    /**
     * Convert PGvector object to float array.
     */
    public float[] pgVectorToArray(PGvector pgVector) {
        if (pgVector == null) {
            return new float[0];
        }
        return pgVector.toArray();
    }
    
    /**
     * Convert float array to PGvector object.
     */
    public PGvector arrayToPgVector(float[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        
        List<Float> list = new ArrayList<>();
        for (float value : array) {
            list.add(value);
        }
        
        return new PGvector(list);
    }
    
    /**
     * Calculate cosine similarity between two vectors.
     * For debugging/testing purposes (use pgvector for production).
     */
    public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || 
            vector1.length != vector2.length || vector1.length == 0) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Calculate Euclidean distance between two vectors.
     */
    public double euclideanDistance(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || 
            vector1.length != vector2.length || vector1.length == 0) {
            return Double.MAX_VALUE;
        }
        
        double sum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sum += diff * diff;
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * Normalize a vector to unit length.
     */
    public float[] normalize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return vector;
        }
        
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return vector;
        }
        
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        
        return normalized;
    }
}
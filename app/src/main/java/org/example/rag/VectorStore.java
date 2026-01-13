package org.example.rag;

import java.util.*;

/**
 * In-memory vector storage with cosine similarity search.
 * Simple brute-force approach suitable for small document sets (<100 chunks).
 */
public class VectorStore {

    private final List<VectorEntry> entries = new ArrayList<>();

    /**
     * Add a vector to the store.
     */
    public void add(String id, float[] embedding, String content, String source) {
        // Normalize the embedding for cosine similarity via dot product
        float[] normalized = normalize(embedding);
        entries.add(new VectorEntry(id, normalized, content, source));
    }

    /**
     * Add chunk with its embedding.
     */
    public void addChunk(Chunk chunk, float[] embedding) {
        add(chunk.id(), embedding, chunk.content(), chunk.source());
    }

    /**
     * Search for similar vectors using cosine similarity.
     * 
     * @param queryEmbedding The query vector (will be normalized internally)
     * @param topK           Number of results to return
     * @return List of scored chunks, sorted by similarity descending
     */
    public List<ScoredChunk> search(float[] queryEmbedding, int topK) {
        float[] normalizedQuery = normalize(queryEmbedding);

        // Calculate similarity with all entries
        List<ScoredChunk> results = new ArrayList<>();
        for (VectorEntry entry : entries) {
            double similarity = dotProduct(normalizedQuery, entry.embedding);
            results.add(new ScoredChunk(entry.id, entry.content, entry.source, similarity));
        }

        // Sort by score descending and take top K
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * Search with source filter.
     */
    public List<ScoredChunk> search(float[] queryEmbedding, int topK, String sourceFilter) {
        float[] normalizedQuery = normalize(queryEmbedding);

        List<ScoredChunk> results = new ArrayList<>();
        for (VectorEntry entry : entries) {
            if (sourceFilter != null && !entry.source.equals(sourceFilter)) {
                continue;
            }
            double similarity = dotProduct(normalizedQuery, entry.embedding);
            results.add(new ScoredChunk(entry.id, entry.content, entry.source, similarity));
        }

        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * Get the number of stored vectors.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Clear all stored vectors.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Normalize a vector to unit length.
     */
    private float[] normalize(float[] vec) {
        double norm = 0;
        for (float v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm == 0)
            return vec;

        float[] normalized = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            normalized[i] = (float) (vec[i] / norm);
        }
        return normalized;
    }

    /**
     * Compute dot product of two vectors.
     * For normalized vectors, this equals cosine similarity.
     */
    private double dotProduct(float[] a, float[] b) {
        double sum = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Internal storage class.
     */
    private record VectorEntry(String id, float[] embedding, String content, String source) {
    }
}

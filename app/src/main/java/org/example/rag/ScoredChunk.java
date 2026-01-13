package org.example.rag;

/**
 * Represents a scored document chunk from retrieval.
 */
public record ScoredChunk(
        String id,
        String content,
        String source,
        double score) {
    /**
     * Create a copy with updated score.
     */
    public ScoredChunk withScore(double newScore) {
        return new ScoredChunk(id, content, source, newScore);
    }
}

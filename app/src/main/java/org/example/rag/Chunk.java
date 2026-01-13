package org.example.rag;

/**
 * Represents a document chunk for indexing.
 */
public record Chunk(
        String id,
        String content,
        String source,
        String header) {
}

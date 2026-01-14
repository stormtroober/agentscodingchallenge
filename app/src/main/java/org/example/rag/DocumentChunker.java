package org.example.rag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits markdown documents into semantic chunks based on headers.
 */
public class DocumentChunker {

    private static final int MAX_CHUNK_SIZE = 1000; // characters
    private static final int OVERLAP_SIZE = 100; // characters overlap between chunks

    /**
     * Chunk a markdown document by ## headers.
     * 
     * @param content The full document content
     * @param source  The source file name (e.g., "billing_policy.md")
     * @return List of chunks
     */
    public List<Chunk> chunkDocument(String content, String source) {
        List<Chunk> chunks = new ArrayList<>();

        // Split by ## headers (level 2), preserving ### sub-headers within
        String[] sections = content.split("(?=(?m)^## )");

        int chunkIndex = 0;
        for (String section : sections) {
            if (section.trim().isEmpty())
                continue;

            // Extract header from first line
            String[] lines = section.split("\n", 2);
            String header = lines[0].replaceAll("^##\\s*", "").trim();
            String body = lines.length > 1 ? lines[1] : "";

            // If section is too large, split further
            if (section.length() > MAX_CHUNK_SIZE) {
                List<Chunk> subChunks = splitLargeSection(section, source, header, chunkIndex);
                chunks.addAll(subChunks);
                chunkIndex += subChunks.size();
            } else {
                String id = source + ":" + chunkIndex;
                chunks.add(new Chunk(id, section.trim(), source, header));
                chunkIndex++;
            }
        }

        return chunks;
    }

    /**
     * Split a large section into smaller chunks with overlap.
     */
    private List<Chunk> splitLargeSection(String section, String source, String header, int startIndex) {
        List<Chunk> chunks = new ArrayList<>();

        // Try to split by ### sub-headers first
        String[] subSections = section.split("(?=(?m)^### )");

        if (subSections.length > 1) {
            // Has sub-headers, use those as chunks
            int idx = startIndex;
            for (String subSection : subSections) {
                if (subSection.trim().isEmpty())
                    continue;

                String subHeader = header;
                String[] lines = subSection.split("\n", 2);
                if (lines[0].startsWith("###")) {
                    subHeader = header + " > " + lines[0].replaceAll("^###\\s*", "").trim();
                }

                String id = source + ":" + idx;
                chunks.add(new Chunk(id, subSection.trim(), source, subHeader));
                idx++;
            }
        } else {
            // No sub-headers, split by paragraphs or size
            chunks.addAll(splitBySize(section, source, header, startIndex));
        }

        return chunks;
    }

    /**
     * Split content by approximate size with overlap.
     */
    private List<Chunk> splitBySize(String content, String source, String header, int startIndex) {
        List<Chunk> chunks = new ArrayList<>();

        // Split by double newlines (paragraphs)
        String[] paragraphs = content.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        int idx = startIndex;

        for (String para : paragraphs) {
            if (currentChunk.length() + para.length() > MAX_CHUNK_SIZE && currentChunk.length() > 0) {
                // Save current chunk
                String id = source + ":" + idx;
                chunks.add(new Chunk(id, currentChunk.toString().trim(), source, header));
                idx++;

                // Start new chunk with overlap from end of previous
                String overlap = getOverlap(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(para);
        }

        // Don't forget the last chunk
        if (currentChunk.length() > 0) {
            String id = source + ":" + idx;
            chunks.add(new Chunk(id, currentChunk.toString().trim(), source, header));
        }

        return chunks;
    }

    /**
     * Get overlap text from end of content.
     */
    private String getOverlap(String content) {
        if (content.length() <= OVERLAP_SIZE) {
            return content;
        }
        return content.substring(content.length() - OVERLAP_SIZE);
    }

    /**
     * Load and chunk all documents from resources.
     */
    public Map<String, List<Chunk>> loadAndChunkAll() {
        Map<String, List<Chunk>> allChunks = new HashMap<>();

        String[] docFiles = {
                "/docs/troubleshooting.md",
                "/docs/integration_guide.md",
                "/docs/faq.md",
                "/docs/system_requirements.md",
                "/docs/installation.md",
                "/docs/billing_policy.md"
        };

        for (String docPath : docFiles) {
            try (InputStream is = getClass().getResourceAsStream(docPath)) {
                if (is != null) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    String source = docPath.substring(docPath.lastIndexOf('/') + 1);
                    List<Chunk> chunks = chunkDocument(content, source);
                    allChunks.put(source, chunks);
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load " + docPath);
            }
        }

        return allChunks;
    }

    /**
     * Get all chunks as a flat list.
     */
    public List<Chunk> loadAllChunks() {
        List<Chunk> all = new ArrayList<>();
        loadAndChunkAll().values().forEach(all::addAll);
        return all;
    }
}

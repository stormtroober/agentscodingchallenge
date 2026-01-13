package org.example.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * Tool for retrieving relevant documentation sections.
 * Uses TF-IDF inspired scoring for better relevance ranking.
 */
public class DocumentRetrievalTool implements Tool {
    private final Map<String, String> documents = new HashMap<>();
    private final Map<String, Map<String, Integer>> documentTermFrequency = new HashMap<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private int totalDocuments = 0;

    // Document-friendly names for better output
    private static final Map<String, String> DOC_NAMES = Map.of(
            "/docs/troubleshooting.md", "Troubleshooting Guide",
            "/docs/integration_guide.md", "Integration Guide",
            "/docs/faq.md", "FAQ",
            "/docs/system_requirements.md", "System Requirements",
            "/docs/installation.md", "Installation Guide");

    public DocumentRetrievalTool() {
        loadDocuments();
        buildIndex();
    }

    private void loadDocuments() {
        String[] docFiles = {
                "/docs/troubleshooting.md",
                "/docs/integration_guide.md",
                "/docs/faq.md",
                "/docs/system_requirements.md",
                "/docs/installation.md"
        };

        for (String docPath : docFiles) {
            try (InputStream is = getClass().getResourceAsStream(docPath)) {
                if (is != null) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    documents.put(docPath, content);
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load " + docPath);
            }
        }
    }

    /**
     * Build term frequency index for TF-IDF scoring
     */
    private void buildIndex() {
        for (Map.Entry<String, String> doc : documents.entrySet()) {
            String[] sections = doc.getValue().split("(?=## )");
            for (String section : sections) {
                if (section.trim().isEmpty())
                    continue;

                String sectionId = doc.getKey() + ":" + section.hashCode();
                Map<String, Integer> termFreq = new HashMap<>();

                // Tokenize and count terms
                String[] words = section.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
                Set<String> uniqueTerms = new HashSet<>();

                for (String word : words) {
                    if (word.length() > 2) { // Skip very short words
                        termFreq.merge(word, 1, Integer::sum);
                        uniqueTerms.add(word);
                    }
                }

                documentTermFrequency.put(sectionId, termFreq);
                totalDocuments++;

                // Update document frequency
                for (String term : uniqueTerms) {
                    documentFrequency.merge(term, 1, Integer::sum);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "search_documentation";
    }

    @Override
    public String getDescription() {
        return "Search technical documentation for information related to a query. " +
                "Returns the most relevant sections from troubleshooting guides, integration docs, " +
                "FAQs, system requirements, and installation guides. Results are ranked by relevance.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "The search query to find relevant documentation sections");
        properties.put("query", queryProp);

        schema.put("properties", properties);
        schema.put("required", List.of("query"));

        return schema;
    }

    @Override
    public String execute(Map<String, String> parameters) {
        String query = parameters.get("query");
        if (query == null || query.isEmpty()) {
            return "Error: No search query provided";
        }

        String[] keywords = query.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        List<ScoredSection> scoredSections = new ArrayList<>();

        for (Map.Entry<String, String> doc : documents.entrySet()) {
            String docPath = doc.getKey();
            String content = doc.getValue();

            // Split by sections (## headers)
            String[] sections = content.split("(?=## )");

            for (String section : sections) {
                if (section.trim().isEmpty())
                    continue;

                double score = calculateTfIdfScore(section, keywords);

                if (score > 0) {
                    String highlighted = highlightKeywords(section.trim(), keywords);
                    String docName = DOC_NAMES.getOrDefault(docPath, docPath);
                    scoredSections.add(new ScoredSection(docName, highlighted, score));
                }
            }
        }

        if (scoredSections.isEmpty()) {
            return "No relevant documentation found for query: \"" + query + "\"\n\n" +
                    "📚 Available documentation covers:\n" +
                    "- Troubleshooting (connection issues, authentication, performance)\n" +
                    "- API Integration (authentication, endpoints, webhooks)\n" +
                    "- System Requirements (SDK, self-hosted, browsers)\n" +
                    "- Installation (Linux, Windows, macOS, SDKs)\n" +
                    "- FAQ (general questions, account management, API & security)";
        }

        // Sort by score descending and take top 3
        scoredSections.sort((a, b) -> Double.compare(b.score, a.score));
        int limit = Math.min(3, scoredSections.size());

        StringBuilder results = new StringBuilder();
        results.append("📖 **Documentation Search Results** (showing top ").append(limit).append(" matches)\n\n");

        for (int i = 0; i < limit; i++) {
            ScoredSection ss = scoredSections.get(i);
            results.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            results.append("📄 **Source:** ").append(ss.docName).append("\n");
            results.append("⭐ **Relevance:** ").append(String.format("%.1f", ss.score * 100)).append("%\n\n");
            results.append(ss.content);
            results.append("\n\n");
        }

        return results.toString();
    }

    /**
     * Calculate TF-IDF inspired score for a section
     */
    private double calculateTfIdfScore(String section, String[] queryTerms) {
        String sectionLower = section.toLowerCase();
        String[] words = sectionLower.replaceAll("[^a-z0-9\\s]", " ").split("\\s+");

        // Calculate term frequency for this section
        Map<String, Integer> termFreq = new HashMap<>();
        for (String word : words) {
            if (word.length() > 2) {
                termFreq.merge(word, 1, Integer::sum);
            }
        }

        double score = 0;
        int matchedTerms = 0;

        for (String term : queryTerms) {
            if (term.length() <= 2)
                continue;

            int tf = termFreq.getOrDefault(term, 0);
            if (tf > 0) {
                matchedTerms++;

                // TF: log normalized
                double tfScore = 1 + Math.log(tf);

                // IDF: inverse document frequency
                int df = documentFrequency.getOrDefault(term, 1);
                double idfScore = Math.log((double) totalDocuments / df);

                score += tfScore * idfScore;

                // Bonus for term in header (first line)
                String firstLine = section.split("\n")[0].toLowerCase();
                if (firstLine.contains(term)) {
                    score += 2.0;
                }
            }
        }

        // Bonus for matching multiple query terms
        if (matchedTerms > 1) {
            score *= (1 + 0.2 * matchedTerms);
        }

        // Normalize score to 0-1 range (approximately)
        return Math.min(1.0, score / 10.0);
    }

    /**
     * Highlight keywords in the section content
     */
    private String highlightKeywords(String content, String[] keywords) {
        String result = content;

        for (String keyword : keywords) {
            if (keyword.length() <= 2)
                continue;

            // Case-insensitive replacement with **bold** markers
            Pattern pattern = Pattern.compile("(?i)\\b(" + Pattern.quote(keyword) + ")\\b");
            result = pattern.matcher(result).replaceAll("**$1**");
        }

        return result;
    }

    private static class ScoredSection {
        String docName;
        String content;
        double score;

        ScoredSection(String docName, String content, double score) {
            this.docName = docName;
            this.content = content;
            this.score = score;
        }
    }
}

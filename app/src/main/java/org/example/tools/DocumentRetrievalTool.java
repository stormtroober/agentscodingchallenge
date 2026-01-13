package org.example.tools;

import org.example.rag.HybridRetriever;
import org.example.rag.ScoredChunk;

import java.util.*;

/**
 * Tool for retrieving relevant documentation sections.
 * Uses hybrid RAG pipeline (BM25 + Vector + RRF + Reranking).
 */
public class DocumentRetrievalTool implements Tool {

    private static final HybridRetriever retriever = new HybridRetriever();

    // Document-friendly names for better output
    private static final Map<String, String> DOC_NAMES = Map.of(
            "troubleshooting.md", "Troubleshooting Guide",
            "integration_guide.md", "Integration Guide",
            "faq.md", "FAQ",
            "system_requirements.md", "System Requirements",
            "installation.md", "Installation Guide",
            "billing_policy.md", "Billing Policy");

    public DocumentRetrievalTool() {
        // Lazy initialization - retriever initializes on first use
    }

    @Override
    public String getName() {
        return "search_documentation";
    }

    @Override
    public String getDescription() {
        return "Search technical documentation for information related to a query. " +
                "Returns the most relevant sections from troubleshooting guides, integration docs, " +
                "FAQs, system requirements, and installation guides. Uses hybrid semantic + keyword search.";
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

        // Use hybrid retriever
        // System.out.println("[DocumentRetrievalTool] Executing search for query: \"" +
        // query + "\"");
        List<ScoredChunk> results = retriever.retrieve(query, 3);

        if (results.isEmpty()) {
            return "No relevant documentation found for query: \"" + query + "\"\n\n" +
                    "📚 Available documentation covers:\n" +
                    "- Troubleshooting (connection issues, authentication, performance)\n" +
                    "- API Integration (authentication, endpoints, webhooks)\n" +
                    "- System Requirements (SDK, self-hosted, browsers)\n" +
                    "- Installation (Linux, Windows, macOS, SDKs)\n" +
                    "- FAQ (general questions, account management, API & security)";
        }

        // Format results
        StringBuilder output = new StringBuilder();
        output.append("📖 **Documentation Search Results** (showing top ").append(results.size())
                .append(" matches)\n\n");

        for (ScoredChunk chunk : results) {
            String docName = DOC_NAMES.getOrDefault(chunk.source(), chunk.source());
            output.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            output.append("📄 **Source:** ").append(docName).append("\n");
            output.append("⭐ **Relevance:** ").append(String.format("%.1f", chunk.score() * 10)).append("\n\n");
            output.append(chunk.content());
            output.append("\n\n");
        }

        return output.toString();
    }
}

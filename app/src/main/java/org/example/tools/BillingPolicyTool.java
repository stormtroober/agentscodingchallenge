package org.example.tools;

import org.example.rag.HybridRetriever;
import org.example.rag.ScoredChunk;

import java.util.*;

/**
 * Tool for retrieving billing policy information.
 * Uses hybrid RAG pipeline with source filtering for billing_policy.md.
 */
public class BillingPolicyTool implements Tool {

    private static final HybridRetriever retriever = new HybridRetriever();
    private static final String SOURCE_FILTER = "billing_policy.md";

    public BillingPolicyTool() {
        // Lazy initialization - retriever initializes on first use
    }

    @Override
    public String getName() {
        return "search_billing_policy";
    }

    @Override
    public String getDescription() {
        return "Search the billing policy document for information about plans, pricing, refunds, " +
                "cancellation, payment methods, and billing procedures. Uses semantic search.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description",
                "The topic to search for in the billing policy (e.g., 'refund', 'enterprise plan', 'cancellation')");
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

        // Use hybrid retriever with source filter
        List<ScoredChunk> results = retriever.retrieve(query, 3, SOURCE_FILTER);

        if (results.isEmpty()) {
            return "No relevant billing policy information found for: " + query +
                    "\n\nThe billing policy covers: subscription plans, billing cycles, refunds, " +
                    "cancellation, disputes, payment failures, and taxes.";
        }

        // Format results
        StringBuilder output = new StringBuilder();
        output.append("📋 **Billing Policy Information**\n\n");

        for (ScoredChunk chunk : results) {
            output.append(chunk.content());
            output.append("\n\n---\n\n");
        }

        return output.toString();
    }
}

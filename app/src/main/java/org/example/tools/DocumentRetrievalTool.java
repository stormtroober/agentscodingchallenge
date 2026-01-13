package org.example.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tool for retrieving relevant documentation sections.
 */
public class DocumentRetrievalTool implements Tool {
    private final Map<String, String> documents = new HashMap<>();

    public DocumentRetrievalTool() {
        loadDocuments();
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

    @Override
    public String getName() {
        return "search_documentation";
    }

    @Override
    public String getDescription() {
        return "Search technical documentation for information related to a query. " +
                "Returns relevant sections from troubleshooting guides, integration docs, FAQs, and system requirements.";
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

        String[] keywords = query.toLowerCase().split("\\s+");
        StringBuilder results = new StringBuilder();

        for (Map.Entry<String, String> doc : documents.entrySet()) {
            String docName = doc.getKey();
            String content = doc.getValue();

            // Find relevant sections
            String[] sections = content.split("(?=##\\s)");
            for (String section : sections) {
                String sectionLower = section.toLowerCase();
                boolean relevant = false;
                for (String keyword : keywords) {
                    if (sectionLower.contains(keyword)) {
                        relevant = true;
                        break;
                    }
                }

                if (relevant) {
                    results.append("\n--- From ").append(docName).append(" ---\n");
                    results.append(section.trim());
                    results.append("\n");
                }
            }
        }

        if (results.isEmpty()) {
            return "No relevant documentation found for query: " + query +
                    ". The available documentation covers: troubleshooting, API integration, FAQs, and system requirements.";
        }

        return results.toString();
    }
}

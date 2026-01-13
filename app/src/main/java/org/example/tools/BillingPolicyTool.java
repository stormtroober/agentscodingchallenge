package org.example.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tool for retrieving billing policy information.
 */
public class BillingPolicyTool implements Tool {
    private String policyContent = "";
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "is", "are", "was", "were",
            "be", "been", "being", "in", "on", "at", "to", "for", "with", "from",
            "by", "about", "of", "that", "this", "these", "those", "it", "what",
            "which", "who", "whom", "available", "can", "will", "do", "does"));

    public BillingPolicyTool() {
        loadPolicy();
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    private void loadPolicy() {
        try (InputStream is = getClass().getResourceAsStream("/docs/billing_policy.md")) {
            if (is != null) {
                policyContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load billing_policy.md");
        }
    }

    @Override
    public String getName() {
        return "search_billing_policy";
    }

    @Override
    public String getDescription() {
        return "Search the billing policy document for information about plans, pricing, refunds, " +
                "cancellation, payment methods, and billing procedures.";
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

        if (policyContent.isEmpty()) {
            return "Error: Billing policy document not available";
        }

        // Sanitize query: remove non-alphanumeric characters (except spaces) and
        // convert to lowercase
        String cleanQuery = query.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        String[] keywords = cleanQuery.split("\\s+");

        StringBuilder results = new StringBuilder();
        results.append("📋 **Billing Policy Information**\n\n");

        // Split by sections (## headers) ensuring we don't split sub-headers (###)
        // (?m) enables multiline mode
        // (?=...) is a lookahead (don't consume the delimiter)
        // ^ match start of line
        // ## match exactly two hashes
        // [ ] match a space (use brackets for clarity or just space)
        String[] sections = policyContent.split("(?=(?m)^## )");
        List<ScoredSection> scoredSections = new ArrayList<>();

        for (String section : sections) {
            if (section.trim().isEmpty())
                continue;

            String sectionLower = section.toLowerCase();
            int score = 0;

            for (String keyword : keywords) {
                if (STOP_WORDS.contains(keyword)) {
                    continue;
                }

                // Match exact keyword
                int exactMatches = countOccurrences(sectionLower, keyword);
                score += exactMatches;

                // Handle simple plurals (s)
                if (keyword.endsWith("s")) {
                    String singular = keyword.substring(0, keyword.length() - 1);
                    if (singular.length() > 2) { // Avoid over-stemming short words
                        score += countOccurrences(sectionLower, singular);
                    }
                } else {
                    String plural = keyword + "s";
                    score += countOccurrences(sectionLower, plural);
                }

                // Bonus for keyword (or variations) in header
                String firstLine = section.split("\n")[0].toLowerCase();
                if (firstLine.contains(keyword)) {
                    score += 5; // Increased header weight
                }
            }

            if (score > 0) {
                scoredSections.add(new ScoredSection(section.trim(), score));
            }
        }

        // Sort by score descending and take top 3
        scoredSections.sort((a, b) -> Integer.compare(b.score, a.score));

        int limit = Math.min(3, scoredSections.size());

        if (limit == 0) {
            return "No relevant billing policy information found for: " + query +
                    "\n\nThe billing policy covers: subscription plans, billing cycles, refunds, " +
                    "cancellation, disputes, payment failures, and taxes.";
        }

        for (int i = 0; i < limit; i++) {
            results.append(scoredSections.get(i).content);
            results.append("\n\n---\n\n");
        }

        return results.toString();
    }

    private static class ScoredSection {
        String content;
        int score;

        ScoredSection(String content, int score) {
            this.content = content;
            this.score = score;
        }
    }
}

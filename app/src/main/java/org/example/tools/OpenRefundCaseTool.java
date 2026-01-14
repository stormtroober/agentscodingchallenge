package org.example.tools;

import java.util.*;

/**
 * Tool for opening a refund support case.
 */
public class OpenRefundCaseTool implements Tool {
    private static int caseCounter = 1000;

    @Override
    public String getName() {
        return "open_refund_case";
    }

    @Override
    public String getDescription() {
        return "Opens a refund support case for customers requesting an ACTUAL refund (not just info). " +
                "REQUIRES BOTH: customer_id (email) AND reason - both must be EXPLICITLY provided by user. " +
                "Do NOT infer or make up the reason - if user hasn't stated a reason, ASK for it first. " +
                "Do NOT use for policy questions. Returns case ID and form link for customer.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> customerIdProp = new HashMap<>();
        customerIdProp.put("type", "string");
        customerIdProp.put("description", "The customer ID or email");
        properties.put("customer_id", customerIdProp);

        Map<String, Object> reasonProp = new HashMap<>();
        reasonProp.put("type", "string");
        reasonProp.put("description", "The reason for the refund request");
        properties.put("reason", reasonProp);

        schema.put("properties", properties);
        schema.put("required", List.of("customer_id", "reason"));

        return schema;
    }

    @Override
    public String execute(Map<String, String> parameters) {
        String customerId = parameters.get("customer_id");
        String reason = parameters.get("reason");

        if (customerId == null || customerId.isEmpty()) {
            return "Error: Customer ID is required to open a refund case.";
        }

        int caseId = ++caseCounter;

        String formUrl = String.format("https://support.example.org/refund-form?caseId=REF-%d&auth=%s",
                caseId, UUID.randomUUID().toString().substring(0, 8));

        return String.format(
                "✅ Refund case opened successfully!\n\n" +
                        "**Case Details:**\n" +
                        "- Case ID: REF-%d\n" +
                        "- Customer: %s\n" +
                        "- Reason: %s\n" +
                        "- Status: Pending Customer Action\n\n" +
                        "**Action Required:**\n" +
                        "Please fill out the official refund request form here:\n" +
                        "🔗 [%s](%s)\n\n" +
                        "**Next Steps:**\n" +
                        "The customer must complete this form within 7 business days. " +
                        "Once received, the refund will be processed according to our refund policy.",
                caseId, customerId, reason != null ? reason : "Not specified", formUrl, formUrl);
    }
}

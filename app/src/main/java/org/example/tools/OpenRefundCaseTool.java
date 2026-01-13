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
        return "Opens a new support case for a refund request. Assigns a case ID and sends the customer a refund form to complete.";
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

        return String.format(
                "✅ Refund case opened successfully!\n\n" +
                        "**Case Details:**\n" +
                        "- Case ID: REF-%d\n" +
                        "- Customer: %s\n" +
                        "- Reason: %s\n" +
                        "- Status: Pending Customer Action\n\n" +
                        "**Next Steps:**\n" +
                        "A refund request form has been sent to the customer's email. " +
                        "The customer must complete this form within 7 business days. " +
                        "Once received, the refund will be processed according to our refund policy.",
                caseId, customerId, reason != null ? reason : "Not specified");
    }
}

package org.example.tools;

import java.util.*;

/**
 * Tool for providing refund timeline information based on billing policy.
 */
public class RefundTimelineTool implements Tool {

    @Override
    public String getName() {
        return "get_refund_timeline";
    }

    @Override
    public String getDescription() {
        return "Provides refund processing timelines and eligibility information based on the refund type and customer's billing situation.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> refundTypeProp = new HashMap<>();
        refundTypeProp.put("type", "string");
        refundTypeProp.put("description", "Type of refund: 'full', 'partial', 'prorated', or 'cancellation'");
        refundTypeProp.put("enum", List.of("full", "partial", "prorated", "cancellation"));
        properties.put("refund_type", refundTypeProp);

        Map<String, Object> planTypeProp = new HashMap<>();
        planTypeProp.put("type", "string");
        planTypeProp.put("description", "Customer's plan type: 'basic', 'professional', or 'enterprise'");
        properties.put("plan_type", planTypeProp);

        schema.put("properties", properties);
        // No required fields - can query by plan type alone for general timeline info
        schema.put("required", List.of());

        return schema;
    }

    @Override
    public String execute(Map<String, String> parameters) {
        String refundType = parameters.get("refund_type");
        String planType = parameters.getOrDefault("plan_type", "basic").toLowerCase();

        // If only plan_type is specified (especially Enterprise), give general timeline
        if (refundType == null || refundType.isEmpty()) {
            if (planType.contains("enterprise")) {
                return String.format(
                        "⏱️ **Enterprise Refund Timeline**\n\n" +
                                "**Processing Time:** 2-3 business days (expedited processing)\n\n" +
                                "**Enterprise Benefits:**\n" +
                                "- Expedited processing for all refund types\n" +
                                "- Dedicated support line for urgent matters\n" +
                                "- Priority handling by your account manager\n\n" +
                                "**Refund Types Available:**\n" +
                                "- Full refund: Within 30 days of purchase\n" +
                                "- Partial refund: For unused service when downgrading\n" +
                                "- Prorated credit: When switching plans mid-cycle\n\n" +
                                "**Payment Method Processing:**\n" +
                                "- Credit/Debit Card: 5-10 business days after processing\n" +
                                "- PayPal: 3-5 business days\n" +
                                "- Bank Transfer: 7-14 business days");
            } else {
                // Default to general info for non-enterprise
                refundType = "full";
            }
        }

        refundType = refundType.toLowerCase();

        String timeline;
        String eligibility;
        String notes;

        switch (refundType) {
            case "full" -> {
                timeline = "5-7 business days";
                eligibility = "Available within 14 days of purchase for monthly plans, 30 days for annual plans";
                notes = "Full refunds are processed to the original payment method.";
            }
            case "partial" -> {
                timeline = "Applied as account credit by default (immediate); standard processing if refunded to payment method";
                eligibility = "Available for unused service periods when downgrading plans";
                notes = "Partial refund amount is calculated based on remaining days in the billing cycle.";
            }
            case "prorated" -> {
                timeline = "Applied as account credit by default";
                eligibility = "Automatically applied when switching between plans mid-cycle";
                notes = "Prorated amounts are typically applied as account credit rather than direct refund.";
            }
            case "cancellation" -> {
                timeline = "10-14 business days";
                eligibility = "Service continues until end of current billing period";
                notes = "Cancellation refunds only apply if within the refund eligibility window.";
            }
            default -> {
                timeline = "5-10 business days (varies)";
                eligibility = "Please specify refund type for accurate information";
                notes = "Contact support for specific eligibility determination.";
            }
        }

        // Enterprise plans have expedited processing
        if (planType.contains("enterprise")) {
            timeline = "2-3 business days (expedited for Enterprise)";
        }

        return String.format(
                "⏱️ **Refund Timeline Information**\n\n" +
                        "**Refund Type:** %s\n" +
                        "**Processing Time:** %s\n\n" +
                        "**Eligibility:**\n%s\n\n" +
                        "**Important Notes:**\n%s\n\n" +
                        "**Payment Method Processing Times:**\n" +
                        "- Credit/Debit Card: Appears within 5-10 business days\n" +
                        "- PayPal: 3-5 business days\n" +
                        "- Bank Transfer: 7-14 business days\n\n" +
                        "For urgent matters, Enterprise customers can contact their dedicated support line.",
                refundType.substring(0, 1).toUpperCase() + refundType.substring(1),
                timeline,
                eligibility,
                notes);
    }
}

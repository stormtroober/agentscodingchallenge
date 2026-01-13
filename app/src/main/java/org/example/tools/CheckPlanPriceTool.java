package org.example.tools;

import java.util.*;

/**
 * Tool for checking customer's plan and price details.
 */
public class CheckPlanPriceTool implements Tool {
    // Simulated plan data
    private static final Map<String, Map<String, String>> CUSTOMER_PLANS = new HashMap<>();

    static {
        // Default/demo plans for any customer lookup
        Map<String, String> basicPlan = new HashMap<>();
        basicPlan.put("plan_name", "Basic");
        basicPlan.put("price", "$9.99/month");
        basicPlan.put("billing_cycle", "Monthly");
        basicPlan.put("features", "5 projects, 10GB storage, Email support");
        basicPlan.put("start_date", "2025-01-15");
        basicPlan.put("next_billing", "2026-02-15");
        CUSTOMER_PLANS.put("default_basic", basicPlan);

        Map<String, String> proPlan = new HashMap<>();
        proPlan.put("plan_name", "Professional");
        proPlan.put("price", "$29.99/month");
        proPlan.put("billing_cycle", "Monthly");
        proPlan.put("features", "Unlimited projects, 100GB storage, Priority support, API access");
        proPlan.put("start_date", "2025-06-01");
        proPlan.put("next_billing", "2026-02-01");
        CUSTOMER_PLANS.put("default_pro", proPlan);

        Map<String, String> enterprisePlan = new HashMap<>();
        enterprisePlan.put("plan_name", "Enterprise");
        enterprisePlan.put("price", "$99.99/month");
        enterprisePlan.put("billing_cycle", "Annual (billed monthly)");
        enterprisePlan.put("features", "Unlimited everything, Dedicated support, Custom integrations, SLA");
        enterprisePlan.put("start_date", "2025-03-01");
        enterprisePlan.put("next_billing", "2026-02-01");
        CUSTOMER_PLANS.put("default_enterprise", enterprisePlan);
    }

    @Override
    public String getName() {
        return "check_plan_price";
    }

    @Override
    public String getDescription() {
        return "Retrieves the customer's current subscription plan and pricing details including features, billing cycle, and next billing date.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> customerIdProp = new HashMap<>();
        customerIdProp.put("type", "string");
        customerIdProp.put("description", "The customer ID or email to look up");
        properties.put("customer_id", customerIdProp);

        schema.put("properties", properties);
        schema.put("required", List.of("customer_id"));

        return schema;
    }

    @Override
    public String execute(Map<String, String> parameters) {
        String customerId = parameters.get("customer_id");

        if (customerId == null || customerId.isEmpty()) {
            return "Error: Customer ID is required to check plan details.";
        }

        // Simulate lookup - assign a plan based on simple hash
        String planKey;
        int hash = Math.abs(customerId.hashCode()) % 3;
        planKey = switch (hash) {
            case 0 -> "default_basic";
            case 1 -> "default_pro";
            default -> "default_enterprise";
        };

        Map<String, String> plan = CUSTOMER_PLANS.get(planKey);

        return String.format(
                "📋 **Customer Plan Details**\n\n" +
                        "Customer: %s\n\n" +
                        "**Current Plan:** %s\n" +
                        "**Price:** %s\n" +
                        "**Billing Cycle:** %s\n" +
                        "**Features:** %s\n\n" +
                        "**Billing Information:**\n" +
                        "- Plan Start Date: %s\n" +
                        "- Next Billing Date: %s",
                customerId,
                plan.get("plan_name"),
                plan.get("price"),
                plan.get("billing_cycle"),
                plan.get("features"),
                plan.get("start_date"),
                plan.get("next_billing"));
    }
}

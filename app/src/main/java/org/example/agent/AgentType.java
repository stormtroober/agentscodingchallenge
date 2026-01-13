package org.example.agent;

/**
 * Enum defining agent types for routing.
 */
public enum AgentType {
    TECHNICAL("Technical Specialist - handles technical questions using documentation"),
    BILLING("Billing Specialist - handles billing, refunds, and plan inquiries"),
    UNKNOWN("Unknown - question falls outside available expertise");

    private final String description;

    AgentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

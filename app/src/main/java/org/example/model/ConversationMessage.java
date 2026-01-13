package org.example.model;

/**
 * Represents a single message in the conversation.
 */
public record ConversationMessage(
        String role, // "user" or "assistant"
        String content,
        String agentType // "COORDINATOR", "TECHNICAL", "BILLING", or null
) {
    public static ConversationMessage user(String content) {
        return new ConversationMessage("user", content, null);
    }

    public static ConversationMessage assistant(String content, String agentType) {
        return new ConversationMessage("assistant", content, agentType);
    }
}

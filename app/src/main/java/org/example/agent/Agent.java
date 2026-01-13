package org.example.agent;

import org.example.model.ConversationContext;

/**
 * Base interface for all agents.
 */
public interface Agent {
    /**
     * Process a user message and return a response.
     * 
     * @param userMessage The user's message
     * @param context     The conversation context for multi-turn support
     * @return The agent's response
     */
    String process(String userMessage, ConversationContext context);

    /**
     * Get the agent type.
     */
    AgentType getType();

    /**
     * Get the agent's name for display.
     */
    String getName();
}

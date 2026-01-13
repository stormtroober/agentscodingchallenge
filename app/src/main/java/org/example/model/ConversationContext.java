package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Manages conversation history for multi-turn support.
 */
public class ConversationContext {
    private final List<ConversationMessage> messages = new ArrayList<>();
    private String currentAgentType = null;

    public void addMessage(ConversationMessage message) {
        messages.add(message);
        if (message.agentType() != null) {
            currentAgentType = message.agentType();
        }
    }

    public List<ConversationMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public String getCurrentAgentType() {
        return currentAgentType;
    }

    public void setCurrentAgentType(String agentType) {
        this.currentAgentType = agentType;
    }

    public List<ConversationMessage> getRecentMessages(int count) {
        int start = Math.max(0, messages.size() - count);
        return messages.subList(start, messages.size());
    }

    public void clear() {
        messages.clear();
        currentAgentType = null;
    }
}

package org.example.llm;

import org.example.model.ConversationMessage;
import org.example.tools.Tool;
import java.util.List;

/**
 * Interface for LLM client implementations.
 */
public interface LLMClient {
    /**
     * Generate a response based on conversation history.
     * 
     * @param systemPrompt The system prompt for the agent
     * @param messages     The conversation history
     * @return The LLM's response text
     */
    String chat(String systemPrompt, List<ConversationMessage> messages);

    /**
     * Generate a response with tool/function calling support.
     * 
     * @param systemPrompt The system prompt for the agent
     * @param messages     The conversation history
     * @param tools        Available tools for the agent
     * @return The LLM response, potentially with tool calls
     */
    LLMResponse chatWithTools(String systemPrompt, List<ConversationMessage> messages, List<Tool> tools);
}

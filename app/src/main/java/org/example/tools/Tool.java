package org.example.tools;

import java.util.Map;

/**
 * Interface for agent tools that can be called via LLM function calling.
 */
public interface Tool {
    /**
     * Get the tool name for LLM function calling.
     */
    String getName();

    /**
     * Get the tool description for LLM.
     */
    String getDescription();

    /**
     * Get the JSON schema for parameters.
     */
    Map<String, Object> getParametersSchema();

    /**
     * Execute the tool with given parameters.
     * 
     * @param parameters The parameters from LLM function call
     * @return The result of tool execution
     */
    String execute(Map<String, String> parameters);
}

package org.example.llm;

import java.util.List;
import java.util.Map;

/**
 * Response from LLM, potentially containing tool calls.
 */
public record LLMResponse(
        String text,
        List<ToolCall> toolCalls,
        boolean hasToolCalls) {
    public static LLMResponse textOnly(String text) {
        return new LLMResponse(text, List.of(), false);
    }

    public static LLMResponse withToolCalls(List<ToolCall> toolCalls) {
        return new LLMResponse(null, toolCalls, true);
    }

    public record ToolCall(
            String name,
            Map<String, String> arguments) {
    }
}

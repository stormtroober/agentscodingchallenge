package org.example.model;

/**
 * Result from tool execution.
 */
public record ToolResult(
        String toolName,
        boolean success,
        String result) {
    public static ToolResult success(String toolName, String result) {
        return new ToolResult(toolName, true, result);
    }

    public static ToolResult failure(String toolName, String error) {
        return new ToolResult(toolName, false, error);
    }
}

package org.example.llm;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test per LLMResponse.
 * Verifica la corretta creazione e gestione delle risposte LLM.
 */
public class LLMResponseTest {

    @Test
    public void testTextOnlyResponse() {
        LLMResponse response = LLMResponse.textOnly("Hello, how can I help you?");

        assertNotNull(response);
        assertEquals("Hello, how can I help you?", response.text());
        assertFalse(response.hasToolCalls());
        assertTrue(response.toolCalls().isEmpty());
    }

    @Test
    public void testTextOnlyWithEmptyString() {
        LLMResponse response = LLMResponse.textOnly("");

        assertNotNull(response);
        assertEquals("", response.text());
        assertFalse(response.hasToolCalls());
    }

    @Test
    public void testResponseWithToolCalls() {
        List<LLMResponse.ToolCall> toolCalls = List.of(
                new LLMResponse.ToolCall("search_documentation", Map.of("query", "API error")));

        LLMResponse response = LLMResponse.withToolCalls(toolCalls);

        assertNotNull(response);
        assertTrue(response.hasToolCalls());
        assertNull(response.text());
        assertEquals(1, response.toolCalls().size());
        assertEquals("search_documentation", response.toolCalls().get(0).name());
    }

    @Test
    public void testResponseWithMultipleToolCalls() {
        List<LLMResponse.ToolCall> toolCalls = List.of(
                new LLMResponse.ToolCall("check_plan_price", Map.of("customer_id", "user123")),
                new LLMResponse.ToolCall("get_refund_timeline", Map.of("refund_type", "full")));

        LLMResponse response = LLMResponse.withToolCalls(toolCalls);

        assertNotNull(response);
        assertTrue(response.hasToolCalls());
        assertEquals(2, response.toolCalls().size());
    }

    @Test
    public void testToolCallWithArguments() {
        Map<String, String> args = Map.of(
                "customer_id", "customer@test.com",
                "reason", "Product not working");

        LLMResponse.ToolCall toolCall = new LLMResponse.ToolCall("open_refund_case", args);

        assertEquals("open_refund_case", toolCall.name());
        assertEquals(2, toolCall.arguments().size());
        assertEquals("customer@test.com", toolCall.arguments().get("customer_id"));
        assertEquals("Product not working", toolCall.arguments().get("reason"));
    }

    @Test
    public void testToolCallWithEmptyArguments() {
        LLMResponse.ToolCall toolCall = new LLMResponse.ToolCall("get_refund_timeline", Map.of());

        assertEquals("get_refund_timeline", toolCall.name());
        assertTrue(toolCall.arguments().isEmpty());
    }

    @Test
    public void testResponseRecordEquality() {
        LLMResponse response1 = LLMResponse.textOnly("Hello");
        LLMResponse response2 = LLMResponse.textOnly("Hello");

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    public void testToolCallRecordEquality() {
        LLMResponse.ToolCall call1 = new LLMResponse.ToolCall("test_tool", Map.of("key", "value"));
        LLMResponse.ToolCall call2 = new LLMResponse.ToolCall("test_tool", Map.of("key", "value"));

        assertEquals(call1, call2);
        assertEquals(call1.hashCode(), call2.hashCode());
    }

    @Test
    public void testResponseToString() {
        LLMResponse response = LLMResponse.textOnly("Test message");

        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("Test message"));
    }
}

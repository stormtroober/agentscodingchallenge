package org.example.tools;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

/**
 * Test generali per verificare che tutti i tool implementino correttamente
 * l'interfaccia Tool.
 */
public class ToolsInterfaceTest {

    @Test
    public void testAllToolsImplementInterface() {
        // Verifica che tutti i tool siano istanziabili e implementino Tool
        Tool docTool = new DocumentRetrievalTool();
        Tool planTool = new CheckPlanPriceTool();
        Tool refundCaseTool = new OpenRefundCaseTool();
        Tool timelineTool = new RefundTimelineTool();

        assertNotNull(docTool);
        assertNotNull(planTool);
        assertNotNull(refundCaseTool);
        assertNotNull(timelineTool);
    }

    @Test
    public void testAllToolsHaveNames() {
        List<Tool> tools = List.of(
                new DocumentRetrievalTool(),
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        for (Tool tool : tools) {
            assertNotNull("Tool should have a name", tool.getName());
            assertFalse("Tool name should not be empty", tool.getName().isEmpty());
        }
    }

    @Test
    public void testAllToolsHaveDescriptions() {
        List<Tool> tools = List.of(
                new DocumentRetrievalTool(),
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        for (Tool tool : tools) {
            assertNotNull("Tool should have a description", tool.getDescription());
            assertFalse("Tool description should not be empty", tool.getDescription().isEmpty());
        }
    }

    @Test
    public void testAllToolsHaveParametersSchema() {
        List<Tool> tools = List.of(
                new DocumentRetrievalTool(),
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        for (Tool tool : tools) {
            Map<String, Object> schema = tool.getParametersSchema();
            assertNotNull("Tool should have parameters schema", schema);
            assertEquals("Schema should have type object", "object", schema.get("type"));
            assertNotNull("Schema should have properties", schema.get("properties"));
        }
    }

    @Test
    public void testToolNamesAreUnique() {
        List<Tool> tools = List.of(
                new DocumentRetrievalTool(),
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        long uniqueCount = tools.stream()
                .map(Tool::getName)
                .distinct()
                .count();

        assertEquals("All tool names should be unique", tools.size(), uniqueCount);
    }

    @Test
    public void testToolNamesFollowNamingConvention() {
        List<Tool> tools = List.of(
                new DocumentRetrievalTool(),
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        for (Tool tool : tools) {
            String name = tool.getName();
            assertTrue("Tool name should be snake_case: " + name,
                    name.matches("^[a-z][a-z0-9_]*$"));
        }
    }

    @Test
    public void testDocumentRetrievalToolName() {
        assertEquals("search_documentation", new DocumentRetrievalTool().getName());
    }

    @Test
    public void testCheckPlanPriceToolName() {
        assertEquals("check_plan_price", new CheckPlanPriceTool().getName());
    }

    @Test
    public void testOpenRefundCaseToolName() {
        assertEquals("open_refund_case", new OpenRefundCaseTool().getName());
    }

    @Test
    public void testRefundTimelineToolName() {
        assertEquals("get_refund_timeline", new RefundTimelineTool().getName());
    }

    @Test
    public void testAllToolsExecuteWithEmptyParameters() {
        List<Tool> tools = List.of(
                new DocumentRetrievalTool(),
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        for (Tool tool : tools) {
            // Dovrebbero gestire i parametri vuoti senza crash
            String result = tool.execute(Map.of());
            assertNotNull("Tool should return result even with empty params: " + tool.getName(), result);
        }
    }
}

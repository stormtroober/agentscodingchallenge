package org.example.tools;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;

public class ToolsTest {

    @Test
    public void testCheckPlanPriceTool() {
        CheckPlanPriceTool tool = new CheckPlanPriceTool();
        // Provide required parameter
        String result = tool.execute(Map.of("customer_id", "test_user"));

        assertNotNull(result);
        assertTrue(result.contains("Customer Plan Details"));
        assertTrue(result.contains("Current Plan"));
    }

    @Test
    public void testRefundTimelineTool() {
        RefundTimelineTool tool = new RefundTimelineTool();
        String result = tool.execute(Map.of());

        assertNotNull(result);
        // Checking generic "refund" keyword presence
        assertTrue(result.toLowerCase().contains("refund"));
    }

    @Test
    public void testOpenRefundCaseTool() {
        OpenRefundCaseTool tool = new OpenRefundCaseTool();
        String result = tool.execute(Map.of("reason", "too expensive"));

        assertNotNull(result);
        // Checking generic keyword presence
        assertTrue(result.toLowerCase().contains("refund") || result.toLowerCase().contains("case"));
    }
}

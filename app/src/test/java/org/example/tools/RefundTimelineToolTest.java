package org.example.tools;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test completo per RefundTimelineTool.
 * Verifica le informazioni sulle tempistiche di rimborso.
 */
public class RefundTimelineToolTest {

    private RefundTimelineTool tool;

    @Before
    public void setUp() {
        tool = new RefundTimelineTool();
    }

    @Test
    public void testToolNameAndDescription() {
        assertEquals("get_refund_timeline", tool.getName());
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().toLowerCase().contains("refund"));
    }

    @Test
    public void testParametersSchema() {
        Map<String, Object> schema = tool.getParametersSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("refund_type"));
    }

    @Test
    public void testFullRefundTimeline() {
        String result = tool.execute(Map.of("refund_type", "full"));

        assertNotNull(result);
        assertTrue(result.contains("Refund Timeline"));
        assertTrue(result.contains("Full") || result.contains("full"));
        assertTrue(result.contains("5-7 business days"));
        assertTrue(result.contains("14 days") || result.contains("30 days"));
    }

    @Test
    public void testPartialRefundTimeline() {
        String result = tool.execute(Map.of("refund_type", "partial"));

        assertNotNull(result);
        assertTrue(result.contains("Partial") || result.contains("partial"));
        assertTrue(result.contains("7-10 business days"));
        assertTrue(result.toLowerCase().contains("downgrad"));
    }

    @Test
    public void testProratedRefundTimeline() {
        String result = tool.execute(Map.of("refund_type", "prorated"));

        assertNotNull(result);
        assertTrue(result.contains("Prorated") || result.contains("prorated"));
        assertTrue(result.contains("1-2 business days"));
        assertTrue(result.toLowerCase().contains("credit"));
    }

    @Test
    public void testCancellationRefundTimeline() {
        String result = tool.execute(Map.of("refund_type", "cancellation"));

        assertNotNull(result);
        assertTrue(result.contains("Cancellation") || result.contains("cancellation"));
        assertTrue(result.contains("10-14 business days"));
    }

    @Test
    public void testDefaultRefundTypeWhenNotProvided() {
        String result = tool.execute(new HashMap<>());

        assertNotNull(result);
        // Dovrebbe usare "full" come default
        assertTrue(result.contains("Refund Timeline"));
        assertTrue(result.contains("business days"));
    }

    @Test
    public void testEnterpriseExpeditedProcessing() {
        String result = tool.execute(Map.of(
                "refund_type", "full",
                "plan_type", "enterprise"));

        assertNotNull(result);
        // Enterprise dovrebbe avere elaborazione accelerata
        assertTrue(result.contains("2-3 business days") || result.contains("expedited"));
    }

    @Test
    public void testBasicPlanProcessing() {
        String result = tool.execute(Map.of(
                "refund_type", "full",
                "plan_type", "basic"));

        assertNotNull(result);
        assertTrue(result.contains("business days"));
        // Non dovrebbe avere elaborazione accelerata
        assertFalse(result.contains("expedited for Enterprise"));
    }

    @Test
    public void testProfessionalPlanProcessing() {
        String result = tool.execute(Map.of(
                "refund_type", "partial",
                "plan_type", "professional"));

        assertNotNull(result);
        assertTrue(result.contains("7-10 business days"));
    }

    @Test
    public void testResultContainsPaymentMethodTimes() {
        String result = tool.execute(Map.of("refund_type", "full"));

        assertNotNull(result);
        assertTrue(result.contains("Payment Method") || result.contains("Credit/Debit Card"));
        assertTrue(result.contains("PayPal") || result.contains("Bank Transfer"));
    }

    @Test
    public void testResultContainsEligibilityInfo() {
        String result = tool.execute(Map.of("refund_type", "full"));

        assertNotNull(result);
        assertTrue(result.contains("Eligibility"));
    }

    @Test
    public void testResultContainsImportantNotes() {
        String result = tool.execute(Map.of("refund_type", "partial"));

        assertNotNull(result);
        assertTrue(result.contains("Important Notes") || result.contains("Notes"));
    }

    @Test
    public void testCaseInsensitiveRefundType() {
        String resultLower = tool.execute(Map.of("refund_type", "full"));
        String resultUpper = tool.execute(Map.of("refund_type", "FULL"));
        String resultMixed = tool.execute(Map.of("refund_type", "Full"));

        // Tutti dovrebbero restituire risultati validi con le stesse tempistiche
        assertNotNull(resultLower);
        assertNotNull(resultUpper);
        assertNotNull(resultMixed);

        assertTrue(resultLower.contains("5-7 business days") || resultLower.contains("Full"));
        assertTrue(resultUpper.contains("5-7 business days") || resultUpper.contains("Full"));
        assertTrue(resultMixed.contains("5-7 business days") || resultMixed.contains("Full"));
    }

    @Test
    public void testUnknownRefundType() {
        String result = tool.execute(Map.of("refund_type", "unknown_type"));

        assertNotNull(result);
        // Dovrebbe comunque restituire qualcosa con informazioni generiche
        assertTrue(result.contains("business days") || result.contains("refund"));
    }
}

package org.example.tools;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test completo per OpenRefundCaseTool.
 * Verifica l'apertura di casi di rimborso.
 */
public class OpenRefundCaseToolTest {

    private OpenRefundCaseTool tool;

    @Before
    public void setUp() {
        tool = new OpenRefundCaseTool();
    }

    @Test
    public void testToolNameAndDescription() {
        assertEquals("open_refund_case", tool.getName());
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
        assertTrue(properties.containsKey("customer_id"));
        assertTrue(properties.containsKey("reason"));
    }

    @Test
    public void testExecuteWithValidParameters() {
        Map<String, String> params = Map.of(
                "customer_id", "customer123@email.com",
                "reason", "Service not as expected");

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Refund case opened successfully") || result.contains("Case"));
        assertTrue(result.contains("REF-")); // Case ID format
        assertTrue(result.contains("customer123@email.com"));
        assertTrue(result.contains("Service not as expected"));
    }

    @Test
    public void testExecuteWithoutCustomerId() {
        Map<String, String> params = Map.of("reason", "Test reason");

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("required"));
    }

    @Test
    public void testExecuteWithEmptyCustomerId() {
        Map<String, String> params = new HashMap<>();
        params.put("customer_id", "");
        params.put("reason", "Test reason");

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("required"));
    }

    @Test
    public void testExecuteWithoutReason() {
        Map<String, String> params = Map.of("customer_id", "customer@test.com");

        String result = tool.execute(params);

        assertNotNull(result);
        // Dovrebbe funzionare anche senza motivo, ma indicare "Not specified"
        assertTrue(result.contains("Case") || result.contains("REF-"));
        assertTrue(result.contains("Not specified") || result.contains("customer@test.com"));
    }

    @Test
    public void testCaseIdIncrementsForEachCase() {
        String result1 = tool.execute(Map.of(
                "customer_id", "user1@test.com",
                "reason", "Reason 1"));
        String result2 = tool.execute(Map.of(
                "customer_id", "user2@test.com",
                "reason", "Reason 2"));

        assertNotNull(result1);
        assertNotNull(result2);

        // Estrai gli ID dei casi
        // Il formato è REF-XXXX dove XXXX è un numero
        assertTrue(result1.contains("REF-"));
        assertTrue(result2.contains("REF-"));

        // I due ID dovrebbero essere diversi
        assertNotEquals("Each case should have unique ID", result1, result2);
    }

    @Test
    public void testResultContainsNextSteps() {
        Map<String, String> params = Map.of(
                "customer_id", "customer@test.com",
                "reason", "Want refund");

        String result = tool.execute(params);

        assertNotNull(result);
        // Dovrebbe contenere informazioni sui prossimi passi
        assertTrue(result.contains("Next Steps") || result.contains("form") || result.contains("business days"));
    }

    @Test
    public void testResultContainsStatus() {
        Map<String, String> params = Map.of(
                "customer_id", "customer@test.com",
                "reason", "Refund request");

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Status") || result.contains("Pending"));
    }

    @Test
    public void testResultContainsCaseDetails() {
        Map<String, String> params = Map.of(
                "customer_id", "john.doe@example.com",
                "reason", "Product not working as advertised");

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Case Details") || result.contains("Case ID"));
        assertTrue(result.contains("john.doe@example.com"));
        assertTrue(result.contains("Product not working"));
    }
}

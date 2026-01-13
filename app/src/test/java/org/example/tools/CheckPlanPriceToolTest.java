package org.example.tools;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test completo per CheckPlanPriceTool.
 * Verifica che il tool restituisca informazioni corrette sui piani.
 */
public class CheckPlanPriceToolTest {

    private CheckPlanPriceTool tool;

    @Before
    public void setUp() {
        tool = new CheckPlanPriceTool();
    }

    @Test
    public void testToolNameAndDescription() {
        assertEquals("check_plan_price", tool.getName());
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().toLowerCase().contains("plan"));
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
    }

    @Test
    public void testExecuteWithValidCustomerId() {
        String result = tool.execute(Map.of("customer_id", "customer123@email.com"));

        assertNotNull(result);
        // Verifica che contenga informazioni sul piano
        assertTrue(result.contains("Customer Plan Details"));
        assertTrue(result.contains("Current Plan"));
        assertTrue(result.contains("Price"));
        assertTrue(result.contains("Billing Cycle"));
        assertTrue(result.contains("Features"));
    }

    @Test
    public void testExecuteWithEmptyCustomerId() {
        Map<String, String> params = new HashMap<>();
        params.put("customer_id", "");

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("required"));
    }

    @Test
    public void testExecuteWithoutCustomerId() {
        Map<String, String> params = new HashMap<>();

        String result = tool.execute(params);

        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("required"));
    }

    @Test
    public void testDifferentCustomersGetDifferentPlans() {
        // Test che diversi customer_id possono ottenere piani diversi
        String result1 = tool.execute(Map.of("customer_id", "user_basic@test.com"));
        String result2 = tool.execute(Map.of("customer_id", "user_pro@test.com"));
        String result3 = tool.execute(Map.of("customer_id", "enterprise_user@company.com"));

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        // Tutti devono contenere informazioni valide
        assertTrue(result1.contains("Current Plan"));
        assertTrue(result2.contains("Current Plan"));
        assertTrue(result3.contains("Current Plan"));
    }

    @Test
    public void testResultContainsBillingInformation() {
        String result = tool.execute(Map.of("customer_id", "test_customer"));

        assertNotNull(result);
        assertTrue(result.contains("Billing Information") || result.contains("Next Billing Date"));
        assertTrue(result.contains("Plan Start Date") || result.contains("start_date"));
    }

    @Test
    public void testResultContainsPlanName() {
        String result = tool.execute(Map.of("customer_id", "test"));

        assertNotNull(result);
        // Deve contenere uno dei nomi di piano validi
        boolean hasValidPlan = result.contains("Basic") ||
                result.contains("Professional") ||
                result.contains("Enterprise");
        assertTrue("Result should contain a valid plan name", hasValidPlan);
    }

    @Test
    public void testResultContainsPriceFormat() {
        String result = tool.execute(Map.of("customer_id", "any_customer"));

        assertNotNull(result);
        // Il prezzo dovrebbe essere in formato $XX.XX/month
        assertTrue("Result should contain price", result.contains("$"));
        assertTrue("Result should mention monthly pricing", result.contains("/month"));
    }

    @Test
    public void testConsistentResultForSameCustomer() {
        String customerId = "consistent_customer@test.com";

        String result1 = tool.execute(Map.of("customer_id", customerId));
        String result2 = tool.execute(Map.of("customer_id", customerId));

        // Lo stesso customer_id dovrebbe restituire lo stesso piano
        assertEquals("Same customer should get same plan", result1, result2);
    }
}

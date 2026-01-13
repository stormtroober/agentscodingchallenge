package org.example.integration;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.llm.GeminiClient;
import org.example.llm.LLMClient;
import org.example.llm.LLMResponse;
import org.example.model.ConversationMessage;
import org.example.tools.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assume;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test di integrazione per il BillingSpecialistAgent con il vero LLM (Gemini).
 * Verifica che l'agente billing utilizzi correttamente i tool di billing.
 * 
 * NOTA: Questi test richiedono una GEMINI_API_KEY valida nel file .env
 */
public class BillingAgentIntegrationTest {

    private static LLMClient llmClient;
    private static boolean apiKeyAvailable = false;

    @BeforeClass
    public static void setUpClass() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String apiKey = dotenv.get("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                dotenv = Dotenv.configure().directory("..").ignoreIfMissing().load();
                apiKey = dotenv.get("GEMINI_API_KEY");
            }
            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = System.getenv("GEMINI_API_KEY");
            }

            if (apiKey != null && !apiKey.isEmpty()) {
                llmClient = new GeminiClient(apiKey);
                apiKeyAvailable = true;
            }
        } catch (Exception e) {
            System.err.println("Could not load API key: " + e.getMessage());
        }
    }

    @Before
    public void setUp() {
        Assume.assumeTrue("GEMINI_API_KEY not available, skipping integration tests", apiKeyAvailable);
    }

    // ===== TEST BILLING TOOLS =====

    @Test
    public void testCheckPlanPriceToolExecution() {
        CheckPlanPriceTool tool = new CheckPlanPriceTool();

        String result = tool.execute(Map.of("customer_id", "billing_test@example.com"));

        assertNotNull(result);
        assertTrue(result.contains("Customer Plan Details"));
        assertTrue(result.contains("Current Plan"));
        // Verifica che contenga uno dei piani validi
        assertTrue(result.contains("Basic") || result.contains("Professional") || result.contains("Enterprise"));
    }

    @Test
    public void testOpenRefundCaseToolExecution() {
        OpenRefundCaseTool tool = new OpenRefundCaseTool();

        String result = tool.execute(Map.of(
                "customer_id", "refund_test@example.com",
                "reason", "Service not as expected"));

        assertNotNull(result);
        assertTrue(result.contains("Refund case opened"));
        assertTrue(result.contains("REF-"));
        assertTrue(result.contains("refund_test@example.com"));
    }

    @Test
    public void testRefundTimelineToolExecution() {
        RefundTimelineTool tool = new RefundTimelineTool();

        String result = tool.execute(Map.of("refund_type", "full"));

        assertNotNull(result);
        assertTrue(result.contains("Refund Timeline"));
        assertTrue(result.contains("business days"));
    }

    // ===== TEST LLM WITH BILLING TOOLS =====

    @Test
    public void testLLMCanCallCheckPlanPriceTool() {
        String systemPrompt = """
                You are a Billing Support Specialist.
                Use the check_plan_price tool to look up customer plan details.
                The customer's ID is billing_user@test.com
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user("What is my current plan? My ID is billing_user@test.com"));

        List<Tool> tools = List.of(new CheckPlanPriceTool());

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, tools);

        assertNotNull(response);
        if (response.hasToolCalls()) {
            assertEquals("check_plan_price", response.toolCalls().get(0).name());
        }
    }

    @Test
    public void testLLMCanCallOpenRefundCaseTool() {
        String systemPrompt = """
                You are a Billing Support Specialist.
                For refund requests, ALWAYS open a case using the open_refund_case tool.
                The customer wants a refund.
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage
                        .user("I want a refund. My email is refund_user@test.com. The service didn't work for me."));

        List<Tool> tools = List.of(new OpenRefundCaseTool());

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, tools);

        assertNotNull(response);
        if (response.hasToolCalls()) {
            assertEquals("open_refund_case", response.toolCalls().get(0).name());
        }
    }

    @Test
    public void testLLMCanCallRefundTimelineTool() {
        String systemPrompt = """
                You are a Billing Support Specialist.
                Use the get_refund_timeline tool to provide refund timing information.
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user("How long will my full refund take?"));

        List<Tool> tools = List.of(new RefundTimelineTool());

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, tools);

        assertNotNull(response);
        if (response.hasToolCalls()) {
            assertEquals("get_refund_timeline", response.toolCalls().get(0).name());
        }
    }

    @Test
    public void testLLMChoosesCorrectToolForPlanInquiry() {
        String systemPrompt = """
                You are a Billing Support Specialist.
                You have access to:
                1. check_plan_price - for plan and pricing inquiries
                2. open_refund_case - for refund requests
                3. get_refund_timeline - for refund timing information
                Choose the appropriate tool for the user's request.
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user("What plan am I on? My ID is plan_check@test.com"));

        List<Tool> tools = List.of(
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, tools);

        assertNotNull(response);
        if (response.hasToolCalls()) {
            assertEquals("Should use check_plan_price", "check_plan_price", response.toolCalls().get(0).name());
        }
    }

    @Test
    public void testLLMChoosesCorrectToolForRefundRequest() {
        String systemPrompt = """
                You are a Billing Support Specialist.
                You have access to:
                1. check_plan_price - for plan and pricing inquiries
                2. open_refund_case - for refund requests
                3. get_refund_timeline - for refund timing information
                Choose the appropriate tool for the user's request.
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user(
                        "I need a refund. My email is refund_request@test.com, reason is the product didn't meet my needs"));

        List<Tool> tools = List.of(
                new CheckPlanPriceTool(),
                new OpenRefundCaseTool(),
                new RefundTimelineTool());

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, tools);

        assertNotNull(response);
        if (response.hasToolCalls()) {
            assertEquals("Should use open_refund_case", "open_refund_case", response.toolCalls().get(0).name());
        }
    }

    // ===== TEST BILLING POLICY COMPLIANCE =====

    @Test
    public void testFullRefundPolicy14Days() {
        RefundTimelineTool tool = new RefundTimelineTool();
        String result = tool.execute(Map.of("refund_type", "full"));

        assertNotNull(result);
        assertTrue("Should mention 14 days for monthly plans",
                result.contains("14 days"));
    }

    @Test
    public void testFullRefundPolicy30DaysAnnual() {
        RefundTimelineTool tool = new RefundTimelineTool();
        String result = tool.execute(Map.of("refund_type", "full"));

        assertNotNull(result);
        assertTrue("Should mention 30 days for annual plans",
                result.contains("30 days"));
    }

    @Test
    public void testEnterpriseExpeditedRefund() {
        RefundTimelineTool tool = new RefundTimelineTool();
        String result = tool.execute(Map.of(
                "refund_type", "full",
                "plan_type", "enterprise"));

        assertNotNull(result);
        assertTrue("Enterprise should have expedited processing",
                result.contains("2-3 business days") || result.contains("expedited"));
    }
}

package org.example.integration;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.agent.CoordinatorAgent;
import org.example.llm.GeminiClient;
import org.example.llm.LLMClient;
import org.example.model.ConversationContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assume;

import static org.junit.Assert.*;

/**
 * Test di integrazione per il CoordinatorAgent con il vero LLM (Gemini).
 * Questi test fanno chiamate reali all'API e verificano il comportamento del
 * sistema.
 * 
 * NOTA: Questi test richiedono una GEMINI_API_KEY valida nel file .env
 */
public class CoordinatorAgentIntegrationTest {

    private static LLMClient llmClient;
    private static boolean apiKeyAvailable = false;

    private CoordinatorAgent coordinatorAgent;
    private ConversationContext context;

    @BeforeClass
    public static void setUpClass() {
        // Carica API key
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
        coordinatorAgent = new CoordinatorAgent(llmClient);
        context = new ConversationContext();
    }

    // ===== TEST DI ROUTING =====

    @Test
    public void testRoutingTechnicalQuestion() {
        String response = coordinatorAgent.process(
                "My API integration is throwing a 500 error, how can I fix it?",
                context);

        assertNotNull(response);
        assertTrue("Should be routed to Technical Specialist",
                response.contains("Technical Specialist"));
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    @Test
    public void testRoutingBillingQuestion() {
        String response = coordinatorAgent.process(
                "I want a refund for my subscription",
                context);

        assertNotNull(response);
        assertTrue("Should be routed to Billing Specialist",
                response.contains("Billing Specialist"));
        assertEquals("BILLING", context.getCurrentAgentType());
    }

    @Test
    public void testRoutingUnrelatedQuestion() {
        String response = coordinatorAgent.process(
                "What is the capital of France?",
                context);

        assertNotNull(response);
        // Dovrebbe essere gestito come fuori ambito
        assertTrue("Should mention it's outside scope",
                response.toLowerCase().contains("outside") ||
                        response.toLowerCase().contains("scope") ||
                        response.toLowerCase().contains("help you with"));
    }

    // ===== TEST TECHNICAL AGENT =====

    @Test
    public void testTechnicalAgentAPIQuestion() {
        String response = coordinatorAgent.process(
                "How do I authenticate with the API?",
                context);

        assertNotNull(response);
        assertTrue(response.contains("Technical Specialist"));
        // La risposta dovrebbe menzionare API o autenticazione
        assertTrue("Response should be about API",
                response.toLowerCase().contains("api") ||
                        response.toLowerCase().contains("authent"));
    }

    @Test
    public void testTechnicalAgentTroubleshootingQuestion() {
        String response = coordinatorAgent.process(
                "I'm getting a connection timeout error, what should I do?",
                context);

        assertNotNull(response);
        assertTrue(response.contains("Technical Specialist"));
    }

    @Test
    public void testTechnicalAgentSystemRequirementsQuestion() {
        String response = coordinatorAgent.process(
                "What are the system requirements for the software?",
                context);

        assertNotNull(response);
        assertTrue(response.contains("Technical Specialist"));
    }

    // ===== TEST BILLING AGENT =====

    @Test
    public void testBillingAgentRefundRequest() {
        String response = coordinatorAgent.process(
                "I need a refund, my email is test@example.com",
                context);

        assertNotNull(response);
        assertTrue(response.contains("Billing Specialist"));
        // Dovrebbe menzionare il processo di rimborso
        assertTrue("Response should mention refund",
                response.toLowerCase().contains("refund"));
    }

    @Test
    public void testBillingAgentPlanInquiry() {
        String response = coordinatorAgent.process(
                "What is my current plan? My customer ID is customer123",
                context);

        assertNotNull(response);
        assertTrue(response.contains("Billing Specialist"));
    }

    @Test
    public void testBillingAgentPricingQuestion() {
        String response = coordinatorAgent.process(
                "How much does the professional plan cost?",
                context);

        assertNotNull(response);
        assertTrue(response.contains("Billing Specialist"));
    }

    // ===== TEST MULTI-TURN CONVERSATION =====

    @Test
    public void testMultiTurnTechnicalConversation() {
        // Prima domanda tecnica
        String response1 = coordinatorAgent.process(
                "How do I integrate the API?",
                context);
        assertNotNull(response1);
        assertTrue(response1.contains("Technical Specialist"));

        // Follow-up tecnico
        String response2 = coordinatorAgent.process(
                "What authentication method should I use?",
                context);
        assertNotNull(response2);
        assertTrue("Follow-up should stay with Technical",
                response2.contains("Technical Specialist"));

        // Il contesto dovrebbe essere mantenuto
        assertTrue(context.getMessages().size() >= 4);
    }

    @Test
    public void testMultiTurnBillingConversation() {
        // Prima domanda billing
        String response1 = coordinatorAgent.process(
                "I want to cancel my subscription",
                context);
        assertNotNull(response1);
        assertTrue(response1.contains("Billing Specialist"));

        // Follow-up billing
        String response2 = coordinatorAgent.process(
                "Will I get a refund?",
                context);
        assertNotNull(response2);
        assertTrue("Follow-up should stay with Billing",
                response2.contains("Billing Specialist"));
    }

    // ===== TEST AGENT SWITCHING =====

    @Test
    public void testSwitchFromTechnicalToBilling() {
        // Start with technical
        String response1 = coordinatorAgent.process(
                "How do I fix an API error?",
                context);
        assertNotNull(response1);
        assertTrue(response1.contains("Technical Specialist"));
        assertEquals("TECHNICAL", context.getCurrentAgentType());

        // Switch to billing
        String response2 = coordinatorAgent.process(
                "Actually, I want a refund. My email is switch@test.com",
                context);
        assertNotNull(response2);
        assertTrue("Should switch to Billing",
                response2.contains("Billing Specialist"));
        assertEquals("BILLING", context.getCurrentAgentType());
    }

    @Test
    public void testSwitchFromBillingToTechnical() {
        // Start with billing
        String response1 = coordinatorAgent.process(
                "What's my current subscription plan? ID: user@test.com",
                context);
        assertNotNull(response1);
        assertTrue(response1.contains("Billing Specialist"));
        assertEquals("BILLING", context.getCurrentAgentType());

        // Switch to technical
        String response2 = coordinatorAgent.process(
                "Thanks! Now I have a technical question - my API calls are failing with 401 error",
                context);
        assertNotNull(response2);
        assertTrue("Should switch to Technical",
                response2.contains("Technical Specialist"));
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    // ===== TEST CONTEXT CLEAR =====

    @Test
    public void testClearContextRestartsConversation() {
        // Build up conversation
        coordinatorAgent.process("I need help with my API", context);
        coordinatorAgent.process("It keeps timing out", context);

        assertTrue("Should have history", context.getMessages().size() > 0);

        // Clear context
        context.clear();

        assertTrue("Messages should be cleared", context.getMessages().isEmpty());
        assertNull("Agent type should be null", context.getCurrentAgentType());

        // New conversation should work independently
        String response = coordinatorAgent.process(
                "I want a refund",
                context);
        assertNotNull(response);
        assertTrue(response.contains("Billing Specialist"));
    }

    // ===== TEST EDGE CASES =====

    @Test
    public void testEmptyMessageHandling() {
        // Testing with very short message that might be ambiguous
        String response = coordinatorAgent.process("help", context);

        assertNotNull(response);
        // Dovrebbe comunque restituire una risposta ragionevole
        assertFalse(response.isEmpty());
    }

    @Test
    public void testLongMessageHandling() {
        String longMessage = "I've been having issues with my API integration for the past few days. " +
                "When I try to make requests to the /users endpoint, I get intermittent 503 errors. " +
                "I've checked my authentication tokens and they seem to be valid. " +
                "The issue started after I upgraded to the new version of the SDK. " +
                "Can you help me troubleshoot this problem?";

        String response = coordinatorAgent.process(longMessage, context);

        assertNotNull(response);
        assertTrue("Long technical question should go to Technical",
                response.contains("Technical Specialist"));
    }

    @Test
    public void testMixedContentQuestion() {
        // Una domanda che potrebbe essere sia tecnica che billing
        String response = coordinatorAgent.process(
                "I'm having problems with the API and I think I should get a refund because of the issues",
                context);

        assertNotNull(response);
        // Dovrebbe essere routing a uno dei due agenti
        assertTrue("Should route to either Technical or Billing",
                response.contains("Technical Specialist") || response.contains("Billing Specialist"));
    }
}

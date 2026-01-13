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
 * Test di scenario end-to-end che simulano conversazioni complete reali.
 * Questi test verificano flussi di conversazione completi multi-turn.
 * 
 * NOTA: Questi test richiedono una GEMINI_API_KEY valida nel file .env
 */
public class EndToEndScenarioTest {

    private static LLMClient llmClient;
    private static boolean apiKeyAvailable = false;

    private CoordinatorAgent coordinatorAgent;
    private ConversationContext context;

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
        coordinatorAgent = new CoordinatorAgent(llmClient);
        context = new ConversationContext();
    }

    // ===== SCENARIO 1: Customer con problema tecnico semplice =====

    @Test
    public void testScenario_SimpleTechnicalSupport() {
        System.out.println("\n=== SCENARIO: Simple Technical Support ===");

        // Turn 1: Problema iniziale
        String response1 = coordinatorAgent.process(
                "Hi, I'm getting a 500 error when calling your API",
                context);
        System.out.println("User: Hi, I'm getting a 500 error when calling your API");
        System.out.println("Agent: " + response1);

        assertNotNull(response1);
        assertTrue("Should route to Technical", response1.contains("Technical Specialist"));

        // Turn 2: Dettagli aggiuntivi
        String response2 = coordinatorAgent.process(
                "It happens when I try to authenticate with my API key",
                context);
        System.out.println("\nUser: It happens when I try to authenticate with my API key");
        System.out.println("Agent: " + response2);

        assertNotNull(response2);
        assertTrue("Should stay with Technical", response2.contains("Technical Specialist"));

        // Turn 3: Ringraziamento
        String response3 = coordinatorAgent.process(
                "Thanks, I'll try that!",
                context);
        System.out.println("\nUser: Thanks, I'll try that!");
        System.out.println("Agent: " + response3);

        assertNotNull(response3);

        // Verifica contesto
        assertEquals("Should have correct agent", "TECHNICAL", context.getCurrentAgentType());
        assertTrue("Should have conversation history", context.getMessages().size() >= 6);
    }

    // ===== SCENARIO 2: Customer che richiede rimborso completo =====

    @Test
    public void testScenario_RefundRequest() {
        System.out.println("\n=== SCENARIO: Refund Request ===");

        // Turn 1: Richiesta iniziale di rimborso
        String response1 = coordinatorAgent.process(
                "I want a refund for my subscription. My email is refund_test@example.com",
                context);
        System.out.println("User: I want a refund for my subscription. My email is refund_test@example.com");
        System.out.println("Agent: " + response1);

        assertNotNull(response1);
        assertTrue("Should route to Billing", response1.contains("Billing Specialist"));

        // Turn 2: Motivo del rimborso
        String response2 = coordinatorAgent.process(
                "The service is not working as expected, I've had many issues",
                context);
        System.out.println("\nUser: The service is not working as expected, I've had many issues");
        System.out.println("Agent: " + response2);

        assertNotNull(response2);
        assertTrue("Should stay with Billing", response2.contains("Billing Specialist"));

        // Turn 3: Domanda sulle tempistiche
        String response3 = coordinatorAgent.process(
                "How long will the refund take?",
                context);
        System.out.println("\nUser: How long will the refund take?");
        System.out.println("Agent: " + response3);

        assertNotNull(response3);
        assertTrue("Should stay with Billing", response3.contains("Billing Specialist"));

        assertEquals("BILLING", context.getCurrentAgentType());
    }

    // ===== SCENARIO 3: Customer che passa da tecnico a billing =====

    @Test
    public void testScenario_TechnicalThenBilling() {
        System.out.println("\n=== SCENARIO: Technical Then Billing ===");

        // Turn 1: Problema tecnico
        String response1 = coordinatorAgent.process(
                "My API integration isn't working",
                context);
        System.out.println("User: My API integration isn't working");
        System.out.println("Agent: " + response1);

        assertNotNull(response1);
        assertTrue("Should start with Technical", response1.contains("Technical Specialist"));

        // Turn 2: Frustrazione e richiesta rimborso
        String response2 = coordinatorAgent.process(
                "I've been struggling with this for days. I think I should just get a refund. My ID is frustrated_user@test.com",
                context);
        System.out.println(
                "\nUser: I've been struggling with this for days. I think I should just get a refund. My ID is frustrated_user@test.com");
        System.out.println("Agent: " + response2);

        assertNotNull(response2);
        assertTrue("Should switch to Billing for refund", response2.contains("Billing Specialist"));
        assertEquals("BILLING", context.getCurrentAgentType());
    }

    // ===== SCENARIO 4: Customer che passa da billing a tecnico =====

    @Test
    public void testScenario_BillingThenTechnical() {
        System.out.println("\n=== SCENARIO: Billing Then Technical ===");

        // Turn 1: Domanda sul piano
        String response1 = coordinatorAgent.process(
                "What plan am I on? My customer ID is plan_user@test.com",
                context);
        System.out.println("User: What plan am I on? My customer ID is plan_user@test.com");
        System.out.println("Agent: " + response1);

        assertNotNull(response1);
        assertTrue("Should start with Billing", response1.contains("Billing Specialist"));

        // Turn 2: Domanda tecnica
        String response2 = coordinatorAgent.process(
                "Got it, thanks! Now I have a question - how do I set up the API integration?",
                context);
        System.out.println("\nUser: Got it, thanks! Now I have a question - how do I set up the API integration?");
        System.out.println("Agent: " + response2);

        assertNotNull(response2);
        assertTrue("Should switch to Technical", response2.contains("Technical Specialist"));
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    // ===== SCENARIO 5: Domanda fuori scopo =====

    @Test
    public void testScenario_OutOfScopeQuestion() {
        System.out.println("\n=== SCENARIO: Out of Scope Question ===");

        // Turn 1: Domanda non pertinente
        String response1 = coordinatorAgent.process(
                "What's the weather like today?",
                context);
        System.out.println("User: What's the weather like today?");
        System.out.println("Agent: " + response1);

        assertNotNull(response1);
        // Dovrebbe indicare che è fuori scopo
        assertTrue("Should mention out of scope",
                response1.toLowerCase().contains("outside") ||
                        response1.toLowerCase().contains("scope") ||
                        response1.toLowerCase().contains("help you with") ||
                        response1.toLowerCase().contains("technical") ||
                        response1.toLowerCase().contains("billing"));

        // Turn 2: Ritorno a domanda valida
        String response2 = coordinatorAgent.process(
                "OK, sorry. Can you help me with my API then?",
                context);
        System.out.println("\nUser: OK, sorry. Can you help me with my API then?");
        System.out.println("Agent: " + response2);

        assertNotNull(response2);
        assertTrue("Should route to Technical for API question",
                response2.contains("Technical Specialist"));
    }

    // ===== SCENARIO 6: Conversazione lunga con più switch =====

    @Test
    public void testScenario_ComplexMultiTurnWithMultipleSwitches() {
        System.out.println("\n=== SCENARIO: Complex Multi-Turn Conversation ===");

        // Turn 1: Tecnico
        String r1 = coordinatorAgent.process("How do I integrate your API?", context);
        System.out.println("Turn 1 - User: How do I integrate your API?");
        System.out.println("Agent: " + r1);
        assertTrue(r1.contains("Technical Specialist"));

        // Turn 2: Ancora tecnico
        String r2 = coordinatorAgent.process("What authentication method should I use?", context);
        System.out.println("\nTurn 2 - User: What authentication method should I use?");
        System.out.println("Agent: " + r2);
        assertTrue(r2.contains("Technical Specialist"));

        // Turn 3: Switch a billing
        String r3 = coordinatorAgent.process("By the way, what plan am I on? My ID is complex_user@test.com", context);
        System.out.println("\nTurn 3 - User: By the way, what plan am I on? My ID is complex_user@test.com");
        System.out.println("Agent: " + r3);
        assertTrue(r3.contains("Billing Specialist"));

        // Turn 4: Ancora billing
        String r4 = coordinatorAgent.process("Can I upgrade to a higher plan?", context);
        System.out.println("\nTurn 4 - User: Can I upgrade to a higher plan?");
        System.out.println("Agent: " + r4);
        assertTrue(r4.contains("Billing Specialist"));

        // Turn 5: Switch back a tecnico
        String r5 = coordinatorAgent.process("OK, going back to my integration issue - I'm getting timeout errors now",
                context);
        System.out.println("\nTurn 5 - User: OK, going back to my integration issue - I'm getting timeout errors now");
        System.out.println("Agent: " + r5);
        assertTrue(r5.contains("Technical Specialist"));

        // Verifica che il contesto abbia tracciato tutto
        assertTrue("Should have full conversation history", context.getMessages().size() >= 10);
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    // ===== SCENARIO 7: Customer con verifica piano e successivo rimborso =====

    @Test
    public void testScenario_CheckPlanThenRefund() {
        System.out.println("\n=== SCENARIO: Check Plan Then Request Refund ===");

        // Turn 1: Verifica piano
        String r1 = coordinatorAgent.process(
                "What's my current subscription? My ID is refund_journey@test.com",
                context);
        System.out.println("Turn 1 - User: What's my current subscription? My ID is refund_journey@test.com");
        System.out.println("Agent: " + r1);
        assertTrue(r1.contains("Billing Specialist"));

        // Turn 2: Timeline
        String r2 = coordinatorAgent.process(
                "If I cancel now, how long would a refund take?",
                context);
        System.out.println("\nTurn 2 - User: If I cancel now, how long would a refund take?");
        System.out.println("Agent: " + r2);
        assertTrue(r2.contains("Billing Specialist"));

        // Turn 3: Richiesta rimborso
        String r3 = coordinatorAgent.process(
                "OK, please process my refund. The reason is I'm not using the service anymore",
                context);
        System.out.println(
                "\nTurn 3 - User: OK, please process my refund. The reason is I'm not using the service anymore");
        System.out.println("Agent: " + r3);
        assertTrue(r3.contains("Billing Specialist"));

        assertEquals("BILLING", context.getCurrentAgentType());
    }

    // ===== SCENARIO 8: Domanda ambigua =====

    @Test
    public void testScenario_AmbiguousQuestion() {
        System.out.println("\n=== SCENARIO: Ambiguous Question ===");

        // Domanda che potrebbe essere sia tecnica che di billing
        String response = coordinatorAgent.process(
                "I'm having problems with my account",
                context);
        System.out.println("User: I'm having problems with my account");
        System.out.println("Agent: " + response);

        assertNotNull(response);
        // Dovrebbe essere indirizzato a uno dei due agenti
        assertTrue("Should route to either Technical or Billing",
                response.contains("Technical Specialist") || response.contains("Billing Specialist"));
    }
}

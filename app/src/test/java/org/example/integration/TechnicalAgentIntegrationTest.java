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

import static org.junit.Assert.*;

/**
 * Test di integrazione per il TechnicalSpecialistAgent con il vero LLM
 * (Gemini).
 * Verifica che l'agente tecnico utilizzi correttamente il tool di ricerca
 * documentazione.
 * 
 * NOTA: Questi test richiedono una GEMINI_API_KEY valida nel file .env
 */
public class TechnicalAgentIntegrationTest {

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

    @Test
    public void testLLMCanCallSearchDocumentationTool() {
        String systemPrompt = """
                You are a Technical Support Specialist.
                ALWAYS use the search_documentation tool to find relevant information before answering.
                User question: How do I fix API timeout errors?
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user("How do I fix API timeout errors?"));

        List<Tool> tools = List.of(new DocumentRetrievalTool());

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, tools);

        assertNotNull(response);
        // L'LLM potrebbe rispondere con testo o tool call
        if (response.hasToolCalls()) {
            assertEquals("search_documentation", response.toolCalls().get(0).name());
            assertTrue(response.toolCalls().get(0).arguments().containsKey("query"));
        }
    }

    @Test
    public void testDocumentRetrievalToolReturnsRelevantContent() {
        DocumentRetrievalTool tool = new DocumentRetrievalTool();

        // Cerca contenuti sull'API
        String result = tool.execute(java.util.Map.of("query", "API authentication"));

        assertNotNull(result);
        // Verifica che restituisca contenuto dalla documentazione
        if (!result.contains("No relevant documentation")) {
            assertTrue("Should contain documentation sections",
                    result.contains("---") || result.contains("From"));
        }
    }

    @Test
    public void testDocumentRetrievalForTroubleshooting() {
        DocumentRetrievalTool tool = new DocumentRetrievalTool();

        String result = tool.execute(java.util.Map.of("query", "error troubleshooting"));

        assertNotNull(result);
    }

    @Test
    public void testDocumentRetrievalForIntegration() {
        DocumentRetrievalTool tool = new DocumentRetrievalTool();

        String result = tool.execute(java.util.Map.of("query", "integration setup"));

        assertNotNull(result);
    }

    @Test
    public void testLLMRespondsWithoutToolsWhenNotNeeded() {
        String systemPrompt = "You are a helpful assistant. Greet the user.";

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user("Hello!"));

        // No tools provided
        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, List.of());

        assertNotNull(response);
        assertFalse("Should not have tool calls for greeting", response.hasToolCalls());
        assertNotNull("Should have text response", response.text());
    }

    @Test
    public void testLLMGeneratesRelevantResponse() {
        String systemPrompt = """
                You are a Technical Support Specialist. Answer the following question briefly.
                """;

        List<ConversationMessage> messages = List.of(
                ConversationMessage.user("What programming languages can I use with your API?"));

        LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, List.of());

        assertNotNull(response);

        if (!response.hasToolCalls()) {
            assertNotNull(response.text());
            assertFalse(response.text().isEmpty());
        }
    }
}

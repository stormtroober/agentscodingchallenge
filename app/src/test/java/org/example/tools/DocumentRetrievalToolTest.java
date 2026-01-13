package org.example.tools;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test completo per DocumentRetrievalTool.
 * Verifica che la ricerca nella documentazione funzioni correttamente.
 */
public class DocumentRetrievalToolTest {

    private DocumentRetrievalTool tool;

    @Before
    public void setUp() {
        tool = new DocumentRetrievalTool();
    }

    @Test
    public void testToolNameAndDescription() {
        assertEquals("search_documentation", tool.getName());
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().contains("documentation"));
    }

    @Test
    public void testParametersSchema() {
        Map<String, Object> schema = tool.getParametersSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("query"));
    }

    @Test
    public void testSearchWithEmptyQuery() {
        Map<String, String> params = new HashMap<>();
        params.put("query", "");

        String result = tool.execute(params);
        assertTrue(result.contains("Error") || result.contains("No search query"));
    }

    @Test
    public void testSearchWithNullQuery() {
        Map<String, String> params = new HashMap<>();
        // query is null

        String result = tool.execute(params);
        assertTrue(result.contains("Error") || result.contains("No search query"));
    }

    @Test
    public void testSearchForAPIRelatedContent() {
        Map<String, String> params = Map.of("query", "API integration");
        String result = tool.execute(params);

        // Should find content from integration_guide.md or faq.md
        assertNotNull(result);
        // Se trova documentazione, dovrebbe contenere sezioni
        if (!result.contains("No relevant documentation")) {
            assertTrue(result.contains("---") || result.toLowerCase().contains("api"));
        }
    }

    @Test
    public void testSearchForTroubleshootingContent() {
        Map<String, String> params = Map.of("query", "error troubleshooting");
        String result = tool.execute(params);

        assertNotNull(result);
        // Dovrebbe trovare contenuti dalla documentazione troubleshooting
        if (!result.contains("No relevant documentation")) {
            assertTrue(result.contains("troubleshooting") || result.contains("---"));
        }
    }

    @Test
    public void testSearchForSystemRequirements() {
        Map<String, String> params = Map.of("query", "system requirements memory");
        String result = tool.execute(params);

        assertNotNull(result);
    }

    @Test
    public void testSearchWithNonExistentKeyword() {
        Map<String, String> params = Map.of("query", "xyznonexistentquery123");
        String result = tool.execute(params);

        assertNotNull(result);
        // Dovrebbe restituire messaggio di nessun risultato
        assertTrue(result.contains("No relevant documentation"));
    }

    @Test
    public void testSearchWithMultipleKeywords() {
        Map<String, String> params = Map.of("query", "connection timeout error");
        String result = tool.execute(params);

        assertNotNull(result);
        // Verifica che restituisca qualcosa (documentazione o messaggio)
        assertFalse(result.isEmpty());
    }

    @Test
    public void testSearchCaseInsensitivity() {
        Map<String, String> paramsLower = Map.of("query", "api");
        Map<String, String> paramsUpper = Map.of("query", "API");

        String resultLower = tool.execute(paramsLower);
        String resultUpper = tool.execute(paramsUpper);

        // Entrambe le ricerche dovrebbero essere equivalenti
        assertNotNull(resultLower);
        assertNotNull(resultUpper);
        // Verifica che entrambe trovino o non trovino risultati coerentemente
        boolean lowerFound = !resultLower.contains("No relevant documentation");
        boolean upperFound = !resultUpper.contains("No relevant documentation");
        assertEquals("Ricerche maiuscole/minuscole dovrebbero dare risultati consistenti", lowerFound, upperFound);
    }
}

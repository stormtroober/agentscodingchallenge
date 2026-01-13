package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.agent.CoordinatorAgent;
import org.example.llm.GeminiClient;
import org.example.llm.LLMClient;
import org.example.model.ConversationContext;
import org.example.model.ConversationMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Agent Refiner Test - English Version
 * 
 * Tests the multi-agent system with English inputs from TEST_SET_EN.md
 * 
 * Run all English tests:
 * ./gradlew :app:test -Dtest.id.en=
 * 
 * Run a specific test by ID:
 * ./gradlew :app:test -Dtest.id.en=1.1
 * 
 * Run all chapter tests:
 * ./gradlew :app:test -Dtest.id.en=1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AgentRefinerTestEN {

    private static final String TEST_SET_PATH = "../TEST_SET_EN.md";
    private LLMClient llmClient;
    private CoordinatorAgent agent;
    private List<TestCase> testCases;

    @BeforeAll
    public void setup() throws IOException {
        // Load environment variables
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = dotenv.get("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // Try looking in parent directory
            dotenv = Dotenv.configure().directory("..").ignoreIfMissing().load();
            apiKey = dotenv.get("GEMINI_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        if (apiKey == null) {
            throw new IllegalStateException("GEMINI_API_KEY not found. Tests cannot run.");
        }

        llmClient = new GeminiClient(apiKey);
        agent = new CoordinatorAgent(llmClient);
        testCases = parseTestCases(Paths.get(TEST_SET_PATH));
    }

    /**
     * Generates individual test cases from TEST_SET_EN.md.
     * 
     * Run all tests:
     * ./gradlew :app:test --tests "org.example.AgentRefinerTestEN"
     * 
     * Run a specific test by ID:
     * ./gradlew :app:test -Dtest.id.en=1.1
     * ./gradlew :app:test -Dtest.id.en=2.3
     */
    @TestFactory
    public Collection<DynamicTest> generateTests() {
        List<DynamicTest> dynamicTests = new ArrayList<>();

        // Check if a specific test ID is requested via system property
        String filterTestId = System.getProperty("test.id.en");
        String filterTestIdIt = System.getProperty("test.id");
        String filterTestIdItExplicit = System.getProperty("test.id.it");
        String filterRag = System.getProperty("test.rag");

        // If RAG filter is active, skip all agent tests
        if (filterRag != null && !filterRag.equals("null") && !filterRag.isEmpty()) {
            System.out.println(">>> [EN] Skipping English tests (RAG filter active)");
            return dynamicTests;
        }

        // If Italian filter is set (property exists), skip English tests entirely
        // Note: empty string means "run all Italian tests", still skip English
        boolean italianFilterActive = (filterTestIdIt != null && !filterTestIdIt.equals("null")) ||
                (filterTestIdItExplicit != null && !filterTestIdItExplicit.equals("null"));
        if (italianFilterActive) {
            System.out.println(">>> [EN] Skipping English tests (Italian filter active)");
            return dynamicTests;
        }

        // Debug: show how many test cases were parsed
        System.out.println(">>> [EN] Parsed " + testCases.size() + " test cases from TEST_SET_EN.md");
        System.out.println(">>> [EN] Filter test.id.en = " + filterTestId);

        for (TestCase testCase : testCases) {
            // Skip if filter is set (not null, not "null" string, not empty) and doesn't
            // match
            boolean hasFilter = filterTestId != null && !filterTestId.equals("null") && !filterTestId.isEmpty();
            if (hasFilter) {
                // Support both exact match (1.1) and chapter prefix (1 matches 1.1, 1.2, etc.)
                boolean matches = testCase.id.equals(filterTestId) || testCase.id.startsWith(filterTestId + ".");
                if (!matches) {
                    continue;
                }
            }

            // Create a test name like "[EN 1.1] Refund request with complete data"
            String testName = "[EN " + testCase.id + "] " + testCase.title;

            dynamicTests.add(dynamicTest(testName, () -> runSingleTest(testCase)));
        }

        System.out.println(">>> [EN] Generated " + dynamicTests.size() + " dynamic tests");

        return dynamicTests;
    }

    private void runSingleTest(TestCase testCase) {
        System.out.println(">>> [EN] STARTING TEST CASE: " + testCase.id + " - " + testCase.title);
        ConversationContext context = new ConversationContext();

        int turnIndex = 1;
        for (Turn turn : testCase.turns) {
            System.out.println("   [Turn " + turnIndex + "] Input: " + turn.input);
            long start = System.currentTimeMillis();
            String agentResponse = agent.process(turn.input, context);
            long duration = System.currentTimeMillis() - start;
            System.out.println("   [Turn " + turnIndex + "] Agent responded in " + duration + "ms");
            System.out.println("   [Turn " + turnIndex + "] Response: "
                    + agentResponse.replace("\n", "\n                    "));
            System.out.println("   [Turn " + turnIndex + "] Expected: " + turn.expected);

            // Verify with LLM Judge
            VerificationResult result = verifyWithJudge(testCase, turn, agentResponse);

            if (!result.passed) {
                System.out.println("   [Turn " + turnIndex + "] ❌ FAILED verification: " + result.reason);
                fail("Test " + testCase.id + " failed at Turn " + turnIndex + ": " + result.reason);
            } else {
                System.out.println("   [Turn " + turnIndex + "] ✅ PASSED verification");
            }
            turnIndex++;
        }
        System.out.println(">>> [EN] TEST CASE " + testCase.id + " PASSED ✅");
    }

    private VerificationResult verifyWithJudge(TestCase testCase, Turn turn, String actualResponse) {
        String systemInstruction = "You are an impartial Judge for an AI Agent testing system. Evaluate if the agent output satisfies the expected behavior.";

        String userContent = String.format(
                """
                        Test Case: %s - %s
                        User Input: %s
                        Expected Behavior/Response:
                        %s

                        Actual Agent Response:
                        %s

                        Task:
                        Evaluate if the "Actual Agent Response" semantically satisfies the "Expected Behavior/Response".

                        IMPORTANT GUIDELINES:
                        - Focus on SEMANTIC correctness, not exact wording.
                        - The response should be in English since input was in English.
                        - If the response contains the required information, even with additional helpful details, that's a PASS.
                        - Only FAIL if the response is missing the key expected information or provides incorrect information.
                        - Tool calls: If expected says "calls X tool" but response contains the correct info, assume tool was called (PASS).

                        Format your response strictly as:
                        STATUS: [PASS or FAIL]
                        REASON: [Brief explanation]
                        """,
                testCase.id, testCase.title, turn.input, turn.expected, actualResponse);

        try {
            // Pass the content as a USER message, not just header
            String judgeResponse = llmClient.chat(systemInstruction, List.of(ConversationMessage.user(userContent)));
            boolean passed = judgeResponse.contains("STATUS: PASS");
            String reason = "Unknown";
            if (judgeResponse.contains("REASON:")) {
                reason = judgeResponse.substring(judgeResponse.indexOf("REASON:") + 7).trim();
            }
            return new VerificationResult(passed, reason);
        } catch (Exception e) {
            e.printStackTrace();
            return new VerificationResult(false, "Judge invocation failed: " + e.getMessage());
        }
    }

    // --- Parser Logic ---

    private List<TestCase> parseTestCases(Path path) throws IOException {
        String content = Files.readString(path);
        List<TestCase> cases = new ArrayList<>();

        // Regex to split by test headers (e.g., #### TEST 1.1)
        Pattern testHeaderPattern = Pattern.compile("#### TEST (\\d+\\.\\d+) - (.+)");
        String[] lines = content.split("\\R");

        TestCase currentCase = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher m = testHeaderPattern.matcher(line);
            if (m.find()) {
                if (currentCase != null)
                    cases.add(currentCase);
                currentCase = new TestCase(m.group(1), m.group(2));
                continue;
            }

            if (currentCase != null) {
                // Determine if it's a table based test or standard block
                // For simplicity, we'll try to detect the "Input:" block or Table row

                // Standard block detection
                if (line.startsWith("**Input:**") || line.startsWith("**Turn 1 Input:**")) {
                    // Parse standard blocks (handle multi-turn block format roughly if needed,
                    // but specifically look for code blocks)
                    // Look ahead for code block
                    StringBuilder inputBuilder = new StringBuilder();
                    int j = i + 1;
                    boolean inCodeBlock = false;
                    while (j < lines.length) {
                        String l = lines[j].trim();
                        if (l.startsWith("```")) {
                            if (inCodeBlock) {
                                i = j;
                                break;
                            } // End of block
                            inCodeBlock = true;
                        } else if (inCodeBlock) {
                            inputBuilder.append(l).append("\n");
                        }
                        j++;
                    }
                    String input = inputBuilder.toString().trim();

                    // Now look for expected
                    // Scan forward for "**Expected"
                    String expected = "";
                    for (int k = i + 1; k < lines.length; k++) {
                        if (lines[k].trim().startsWith("#### TEST"))
                            break; // Don't go to next test
                        if (lines[k].trim().startsWith("**Expected")) {
                            // Check if expected is on the same line (e.g., **Expected:** text here)
                            String expLine = lines[k].trim();
                            int colonIdx = expLine.indexOf(":");
                            if (colonIdx != -1 && colonIdx < expLine.length() - 1) {
                                // There's content after the colon on the same line
                                String sameLineContent = expLine.substring(colonIdx + 1).replace("**", "").trim();
                                if (!sameLineContent.isEmpty()) {
                                    expected = sameLineContent;
                                    break;
                                }
                            }

                            // extract expected text until next test, next turn, section divider, or header
                            StringBuilder expBuilder = new StringBuilder();
                            for (int z = k + 1; z < lines.length; z++) {
                                String zLine = lines[z].trim();
                                // Stop conditions
                                if (zLine.startsWith("#### TEST") ||
                                        zLine.startsWith("**Turn") ||
                                        zLine.startsWith("---") ||
                                        zLine.startsWith("###") ||
                                        zLine.startsWith("**Input"))
                                    break;
                                if (!zLine.isEmpty())
                                    expBuilder.append(zLine).append("\n");
                            }
                            expected = expBuilder.toString().trim();
                            break;
                        }
                    }

                    if (!input.isEmpty()) {
                        currentCase.turns.add(new Turn(input, expected));
                    }
                }

                // Table detection (Category 5)
                if (line.startsWith("|") && line.contains("Turn") && line.contains("Input")) {
                    // Table header found, skip separator
                    i++;
                    // Read rows
                    while (i + 1 < lines.length) {
                        String row = lines[++i].trim();
                        if (!row.startsWith("|"))
                            break;
                        String[] parts = row.split("\\|");
                        if (parts.length >= 4) {
                            String input = parts[2].trim().replace("\"", "");
                            String expected = parts[3].trim();
                            currentCase.turns.add(new Turn(input, expected));
                        }
                    }
                }
            }
        }
        if (currentCase != null)
            cases.add(currentCase);

        return cases;
    }

    // --- Helper Classes ---

    private static class TestCase {
        String id;
        String title;
        List<Turn> turns = new ArrayList<>();

        TestCase(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    private static class Turn {
        String input;
        String expected;

        Turn(String input, String expected) {
            this.input = input;
            this.expected = expected;
        }
    }

    private static class VerificationResult {
        boolean passed;
        String reason;

        VerificationResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }
    }
}

/*
 * Multi-Agent Conversational Support System
 */
package org.example;

import org.example.agent.CoordinatorAgent;
import org.example.llm.GeminiClient;
import org.example.llm.LLMClient;
import org.example.model.ConversationContext;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Scanner;

/**
 * Main application entry point for the multi-agent support system.
 */
public class App {
    private static final String WELCOME_MESSAGE = """

            ╔════════════════════════════════════════════════════════════════╗
            ║          Welcome to our Support Chat!                          ║
            ║                                                                ║
            ║  I can help you with:                                          ║
            ║  🔧 Technical questions (features, troubleshooting, APIs)      ║
            ║  💳 Billing inquiries (refunds, plans, payments)               ║
            ║                                                                ║
            ║  Type 'quit' or 'exit' to end the conversation.                ║
            ║  Type 'clear' to start a new conversation.                     ║
            ╚════════════════════════════════════════════════════════════════╝

            """;

    public static void main(String[] args) {
        // Load environment variables - try default then strict load from parent if
        // needed
        Dotenv dotenv;
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
            if (dotenv.get("GEMINI_API_KEY") == null) {
                // Try looking in parent directory (project root)
                dotenv = Dotenv.configure().directory("..").ignoreIfMissing().load();
            }
        } catch (Exception e) {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        }

        // Get API key from .env or system environment
        String apiKey = dotenv.get("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: GEMINI_API_KEY is not set.");
            System.err.println("Please create a .env file with GEMINI_API_KEY=your_key");
            System.err.println("OR set it as an environment variable: export GEMINI_API_KEY=your_key");
            System.exit(1);
        }

        // Initialize components
        LLMClient llmClient = new GeminiClient(apiKey);
        CoordinatorAgent coordinator = new CoordinatorAgent(llmClient);
        ConversationContext context = new ConversationContext();

        System.out.println(WELCOME_MESSAGE);

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n👤 You: ");
                String userInput = scanner.nextLine().trim();

                if (userInput.isEmpty()) {
                    continue;
                }

                if (userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("exit")) {
                    System.out.println("\n👋 Thank you for contacting support. Goodbye!");
                    break;
                }

                if (userInput.equalsIgnoreCase("clear")) {
                    context.clear();
                    System.out.println("\n🔄 Conversation cleared. Starting fresh!\n");
                    continue;
                }

                try {
                    System.out.println("⏳ Processing your request...");
                    long startTime = System.currentTimeMillis();
                    String response = coordinator.process(userInput, context);
                    long endTime = System.currentTimeMillis();
                    double durationSeconds = (endTime - startTime) / 1000.0;

                    System.out.println(response);
                    System.out.printf("\n(⏱️ Response time: %.2fs)%n", durationSeconds);
                } catch (Exception e) {
                    System.err.println("\n❌ Error processing your request: " + e.getMessage());
                    System.err.println("Please try again or type 'clear' to start over.");
                }
            }
        }
    }
}

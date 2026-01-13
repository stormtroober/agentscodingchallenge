package org.example.agent;

import org.example.llm.LLMClient;
import org.example.model.ConversationContext;
import org.example.model.ConversationMessage;

import java.util.List;

/**
 * Coordinator agent that routes messages to appropriate specialist agents.
 */
public class CoordinatorAgent implements Agent {
    private static final String ROUTING_PROMPT = """
            You are a support routing coordinator. Your job is to analyze each customer message
            and determine which specialist should handle it.

            Available specialists:
            1. TECHNICAL - Handles: product features, errors, bugs, integrations, API questions,
               troubleshooting, system requirements, installation, configuration
            2. BILLING - Handles: refunds, payments, subscriptions, plan changes, pricing,
               invoices, account charges, billing disputes
            3. UNKNOWN - Use this ONLY when the question is completely unrelated to our support
               (e.g., weather, sports, general knowledge questions)

            RESPOND WITH EXACTLY ONE WORD: TECHNICAL, BILLING, or UNKNOWN

            Consider the full conversation context when routing. If a follow-up question
            relates to the same topic, keep it with the same specialist.
            """;

    private static final String UNKNOWN_RESPONSE = """
            I appreciate your question, but it falls outside the scope of our support services.

            I can help you with:
            • **Technical questions** - Product features, troubleshooting, integrations, API usage
            • **Billing inquiries** - Refunds, payments, subscriptions, pricing, invoices

            Is there anything related to these areas I can help you with today?
            """;

    private final LLMClient llmClient;
    private final Agent technicalAgent;
    private final Agent billingAgent;

    public CoordinatorAgent(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.technicalAgent = new TechnicalSpecialistAgent(llmClient);
        this.billingAgent = new BillingSpecialistAgent(llmClient);
    }

    private static final String TRANSLATION_PROMPT = """
            You are a precise translator.
            Translate the following user message to English.
            If the message is already in English, return it exactly as is.
            Do not add any explanations, preambles, or extra text. Just the translation.
            """;

    @Override
    public String process(String userMessage, ConversationContext context) {
        // 1. Translate message to English to ensure consistent understanding and tool
        // usage
        String translatedMessage = translateToEnglish(userMessage);

        // 2. Determine which agent should handle this message (using English content)
        AgentType targetAgent = routeMessage(translatedMessage, context);

        String response;
        String respondingAgent;

        switch (targetAgent) {
            case TECHNICAL -> {
                response = technicalAgent.process(translatedMessage, context);
                respondingAgent = "TECHNICAL";
            }
            case BILLING -> {
                response = billingAgent.process(translatedMessage, context);
                respondingAgent = "BILLING";
            }
            default -> {
                response = UNKNOWN_RESPONSE;
                respondingAgent = "COORDINATOR";
            }
        }

        // Update context with the verified English interaction
        // This ensures future context is cleaner for the English-native agents
        context.addMessage(ConversationMessage.user(translatedMessage));
        context.addMessage(ConversationMessage.assistant(response, respondingAgent));
        context.setCurrentAgentType(respondingAgent);

        return formatResponse(response, respondingAgent);
    }

    private String translateToEnglish(String originalMessage) {
        return llmClient.chat(TRANSLATION_PROMPT,
                List.of(ConversationMessage.user(originalMessage))).trim();
    }

    private AgentType routeMessage(String userMessage, ConversationContext context) {
        // Build context for routing decision
        StringBuilder routingContext = new StringBuilder();

        // Include recent conversation history for context
        List<ConversationMessage> recentMessages = context.getRecentMessages(4);
        if (!recentMessages.isEmpty()) {
            routingContext.append("Recent conversation:\n");
            for (ConversationMessage msg : recentMessages) {
                routingContext.append(msg.role()).append(": ").append(msg.content()).append("\n");
            }
            routingContext.append("\nCurrent agent: ").append(context.getCurrentAgentType()).append("\n\n");
        }

        routingContext.append("New message to route: ").append(userMessage);

        String decision = llmClient.chat(ROUTING_PROMPT,
                List.of(ConversationMessage.user(routingContext.toString())));

        decision = decision.trim().toUpperCase();

        // Parse the decision
        if (decision.contains("TECHNICAL")) {
            return AgentType.TECHNICAL;
        } else if (decision.contains("BILLING")) {
            return AgentType.BILLING;
        } else {
            return AgentType.UNKNOWN;
        }
    }

    private String formatResponse(String response, String agentType) {
        String agentLabel = switch (agentType) {
            case "TECHNICAL" -> "🔧 Technical Specialist";
            case "BILLING" -> "💳 Billing Specialist";
            default -> "🤖 Support";
        };

        return "\n[" + agentLabel + "]\n" + response;
    }

    @Override
    public AgentType getType() {
        return null; // Coordinator doesn't have a specific type
    }

    @Override
    public String getName() {
        return "Coordinator";
    }
}

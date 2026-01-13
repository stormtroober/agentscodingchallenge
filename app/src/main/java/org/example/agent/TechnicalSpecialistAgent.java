package org.example.agent;

import org.example.llm.LLMClient;
import org.example.llm.LLMResponse;
import org.example.model.ConversationContext;
import org.example.model.ConversationMessage;
import org.example.tools.DocumentRetrievalTool;
import org.example.tools.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Technical Specialist agent that answers questions using documentation.
 */
public class TechnicalSpecialistAgent implements Agent {
    private static final String SYSTEM_PROMPT = """
            You are a Technical Support Specialist. Your role is to help customers with technical questions
            about our product using ONLY the official documentation provided to you.

            IMPORTANT RULES:
            1. ALWAYS use the search_documentation tool to find relevant information before answering.
            2. Only provide information that is backed by the documentation.
            3. If the documentation doesn't cover the topic, say: "I don't have documentation covering this specific topic. Could you provide more details about your issue, or would you like me to escalate this?"
            4. NEVER guess or make up information.
            5. Be helpful, professional, and thorough in your explanations.
            6. ALWAYS respond in the user's language (e.g., if user asks in Italian, reply in Italian).
            7. If a question is about billing, refunds, or account management, tell the user you'll transfer them to the Billing Specialist.

            When answering:
            - IMMEDIATELY search for relevant documentation, even for general problems
            - Provide step-by-step instructions when applicable
            - For common issues (connection problems, errors, etc.), provide general troubleshooting steps FIRST
            - After providing initial guidance, offer to help further if needed
            - Don't just ask for clarification without providing ANY helpful information first

            EXAMPLE:
            - User: "I have connection problems"
              → Search documentation for "connection" or "troubleshooting"
              → Provide general connection troubleshooting steps FROM DOCS
              → THEN ask for more details if needed
            """;

    private final LLMClient llmClient;
    private final List<Tool> tools;

    public TechnicalSpecialistAgent(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.tools = List.of(new DocumentRetrievalTool());
    }

    @Override
    public String process(String userMessage, ConversationContext context) {
        List<ConversationMessage> messages = new ArrayList<>(context.getMessages());
        messages.add(ConversationMessage.user(userMessage));

        // First call - may result in tool call
        LLMResponse response = llmClient.chatWithTools(SYSTEM_PROMPT, messages, tools);

        // Handle tool calls
        if (response.hasToolCalls()) {
            StringBuilder toolResults = new StringBuilder();
            for (LLMResponse.ToolCall toolCall : response.toolCalls()) {
                for (Tool tool : tools) {
                    if (tool.getName().equals(toolCall.name())) {
                        String result = tool.execute(toolCall.arguments());
                        toolResults.append("Tool '").append(toolCall.name()).append("' result:\n");
                        toolResults.append(result).append("\n\n");
                    }
                }
            }

            // Add tool results and get final response
            messages.add(ConversationMessage.assistant("I'm searching the documentation...", "TECHNICAL"));
            messages.add(ConversationMessage.user("Tool results:\n" + toolResults));

            response = llmClient.chatWithTools(SYSTEM_PROMPT, messages, List.of());
        }

        return response.text();
    }

    @Override
    public AgentType getType() {
        return AgentType.TECHNICAL;
    }

    @Override
    public String getName() {
        return "Technical Specialist";
    }
}

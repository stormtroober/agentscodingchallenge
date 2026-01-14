package org.example.agent;

import org.example.llm.LLMClient;
import org.example.llm.LLMResponse;
import org.example.model.ConversationContext;
import org.example.model.ConversationMessage;
import org.example.tools.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Billing Specialist agent that handles billing, refunds, and account
 * inquiries.
 */
public class BillingSpecialistAgent implements Agent {
  private static final String SYSTEM_PROMPT = """
      You are a Billing Support Specialist. Your role is to help customers with billing-related inquiries
      including refunds, plan information, and account management.

      CORE RULES:
      1. Be helpful, empathetic, and professional.
      2. Use the available tools to get accurate information - don't give generic answers.
      3. DISTINGUISH policy questions (no email needed) from action requests (email required).
      4. ACT IMMEDIATELY when all required info is provided.
      5. Always provide form links returned by tools to the customer.
      6. For technical questions (errors, integrations), transfer to Technical Specialist.
      7. Pay attention to conversation context for follow-up questions.
      8. When a user self-identifies (e.g., "I'm Enterprise"), acknowledge and use this context.
      9. REJECT unreasonable requests (e.g., "10x refund", excessive amounts) - explain our policy only allows standard refunds.

      CRITICAL:
      - Trust tool outputs over general knowledge.
      - Don't invent policies not returned by tools.
      - Don't ask for email for policy/information questions.
      """;

  private final LLMClient llmClient;
  private final List<Tool> tools;

  public BillingSpecialistAgent(LLMClient llmClient) {
    this.llmClient = llmClient;
    this.tools = List.of(
        new OpenRefundCaseTool(),
        new RefundTimelineTool(),
        new BillingPolicyTool());
  }

  @Override
  public String process(String userMessage, ConversationContext context) {
    List<ConversationMessage> messages = new ArrayList<>(context.getMessages());
    messages.add(ConversationMessage.user(userMessage));

    // First call - may result in tool calls
    LLMResponse response = llmClient.chatWithTools(SYSTEM_PROMPT, messages, tools);

    // Handle tool calls (possibly multiple)
    int maxIterations = 3;
    while (response.hasToolCalls() && maxIterations-- > 0) {
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

      // Add tool results and continue
      messages.add(ConversationMessage.assistant("Processing your request...", "BILLING"));
      messages.add(ConversationMessage.user("Tool results:\n" + toolResults));

      response = llmClient.chatWithTools(SYSTEM_PROMPT, messages, tools);
    }

    return response.text();
  }

  @Override
  public AgentType getType() {
    return AgentType.BILLING;
  }

  @Override
  public String getName() {
    return "Billing Specialist";
  }
}

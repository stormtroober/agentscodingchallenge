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

            You have access to the following tools:
            1. open_refund_case - Opens a support case for refund requests and sends customer a form
            2. check_plan_price - Retrieves customer's current plan and pricing details
            3. get_refund_timeline - Provides refund processing timelines based on our policy

            IMPORTANT RULES:
            1. Always be helpful, empathetic, and professional.
            2. Use the appropriate tool to assist customers - don't just give generic answers.
            3. For refund requests, ALWAYS open a case using the open_refund_case tool.
            4. Confirm customer identity (ask for their email/customer ID) before processing requests.
            5. Explain policies clearly and set realistic expectations.
            6. If a question is technical (about product features, errors, integrations), tell the user
               you'll transfer them to the Technical Specialist.

            Our refund policy summary:
            - Full refunds: Within 14 days for monthly, 30 days for annual plans
            - Partial refunds: For unused service periods when downgrading
            - Processing time: 5-14 business days depending on payment method
            """;

    private final LLMClient llmClient;
    private final List<Tool> tools;

    public BillingSpecialistAgent(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.tools = List.of(
                new OpenRefundCaseTool(),
                new CheckPlanPriceTool(),
                new RefundTimelineTool());
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

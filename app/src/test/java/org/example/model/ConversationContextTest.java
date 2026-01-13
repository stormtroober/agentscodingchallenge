package org.example.model;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test completo per ConversationContext.
 * Verifica la gestione del contesto di conversazione multi-turn.
 */
public class ConversationContextTest {

    private ConversationContext context;

    @Before
    public void setUp() {
        context = new ConversationContext();
    }

    @Test
    public void testEmptyContextInitialization() {
        assertTrue(context.getMessages().isEmpty());
        assertNull(context.getCurrentAgentType());
    }

    @Test
    public void testAddUserMessage() {
        ConversationMessage userMsg = ConversationMessage.user("Hello, I need help");
        context.addMessage(userMsg);

        assertEquals(1, context.getMessages().size());
        assertEquals("user", context.getMessages().get(0).role());
        assertEquals("Hello, I need help", context.getMessages().get(0).content());
    }

    @Test
    public void testAddAssistantMessage() {
        ConversationMessage assistantMsg = ConversationMessage.assistant("How can I help you?", "TECHNICAL");
        context.addMessage(assistantMsg);

        assertEquals(1, context.getMessages().size());
        assertEquals("assistant", context.getMessages().get(0).role());
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    @Test
    public void testMultipleTurnsConversation() {
        context.addMessage(ConversationMessage.user("I have an API question"));
        context.addMessage(ConversationMessage.assistant("Sure, what's your question?", "TECHNICAL"));
        context.addMessage(ConversationMessage.user("How do I authenticate?"));
        context.addMessage(ConversationMessage.assistant("You can use OAuth 2.0...", "TECHNICAL"));

        assertEquals(4, context.getMessages().size());
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    @Test
    public void testAgentTypeSwitching() {
        // Start with technical
        context.addMessage(ConversationMessage.user("Fix my API"));
        context.addMessage(ConversationMessage.assistant("API response", "TECHNICAL"));
        assertEquals("TECHNICAL", context.getCurrentAgentType());

        // Switch to billing
        context.addMessage(ConversationMessage.user("I need a refund"));
        context.addMessage(ConversationMessage.assistant("I'll help with refund", "BILLING"));
        assertEquals("BILLING", context.getCurrentAgentType());

        // Switch back to technical
        context.addMessage(ConversationMessage.user("Now another API error"));
        context.addMessage(ConversationMessage.assistant("Let me check", "TECHNICAL"));
        assertEquals("TECHNICAL", context.getCurrentAgentType());
    }

    @Test
    public void testGetRecentMessagesWithFewerThanRequested() {
        context.addMessage(ConversationMessage.user("Message 1"));
        context.addMessage(ConversationMessage.assistant("Reply 1", "TECHNICAL"));

        List<ConversationMessage> recent = context.getRecentMessages(5);
        assertEquals(2, recent.size()); // Only 2 messages exist
    }

    @Test
    public void testGetRecentMessagesWithExactCount() {
        context.addMessage(ConversationMessage.user("Message 1"));
        context.addMessage(ConversationMessage.assistant("Reply 1", "TECHNICAL"));
        context.addMessage(ConversationMessage.user("Message 2"));
        context.addMessage(ConversationMessage.assistant("Reply 2", "BILLING"));

        List<ConversationMessage> recent = context.getRecentMessages(4);
        assertEquals(4, recent.size());
    }

    @Test
    public void testGetRecentMessagesWithMoreThanRequested() {
        context.addMessage(ConversationMessage.user("Message 1"));
        context.addMessage(ConversationMessage.assistant("Reply 1", "TECHNICAL"));
        context.addMessage(ConversationMessage.user("Message 2"));
        context.addMessage(ConversationMessage.assistant("Reply 2", "BILLING"));
        context.addMessage(ConversationMessage.user("Message 3"));
        context.addMessage(ConversationMessage.assistant("Reply 3", "TECHNICAL"));

        List<ConversationMessage> recent = context.getRecentMessages(3);
        assertEquals(3, recent.size());
        // Should be the last 3 messages
        assertEquals("Message 2", recent.get(0).content());
        assertEquals("Reply 2", recent.get(1).content());
    }

    @Test
    public void testClearConversation() {
        context.addMessage(ConversationMessage.user("Hello"));
        context.addMessage(ConversationMessage.assistant("Hi!", "TECHNICAL"));
        context.setCurrentAgentType("BILLING");

        assertFalse(context.getMessages().isEmpty());
        assertNotNull(context.getCurrentAgentType());

        context.clear();

        assertTrue(context.getMessages().isEmpty());
        assertNull(context.getCurrentAgentType());
    }

    @Test
    public void testSetCurrentAgentType() {
        assertNull(context.getCurrentAgentType());

        context.setCurrentAgentType("TECHNICAL");
        assertEquals("TECHNICAL", context.getCurrentAgentType());

        context.setCurrentAgentType("BILLING");
        assertEquals("BILLING", context.getCurrentAgentType());

        context.setCurrentAgentType("COORDINATOR");
        assertEquals("COORDINATOR", context.getCurrentAgentType());
    }

    @Test
    public void testMessagesAreImmutable() {
        context.addMessage(ConversationMessage.user("Test"));

        List<ConversationMessage> messages = context.getMessages();

        // Trying to modify should throw exception
        try {
            messages.add(ConversationMessage.user("Hacked"));
            fail("Should not be able to modify returned list");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testMessageRoleCorrectness() {
        ConversationMessage userMsg = ConversationMessage.user("User content");
        assertEquals("user", userMsg.role());
        assertNull(userMsg.agentType());

        ConversationMessage assistantMsg = ConversationMessage.assistant("Assistant content", "TECHNICAL");
        assertEquals("assistant", assistantMsg.role());
        assertEquals("TECHNICAL", assistantMsg.agentType());
    }

    @Test
    public void testAgentTypeNullForUserMessages() {
        ConversationMessage userMsg = ConversationMessage.user("Hello");
        context.addMessage(userMsg);

        // Agent type should not change when user message has null agentType
        assertNull(context.getCurrentAgentType());
    }

    @Test
    public void testLongConversationHistory() {
        // Simulate a long conversation
        for (int i = 0; i < 50; i++) {
            context.addMessage(ConversationMessage.user("Question " + i));
            String agentType = i % 2 == 0 ? "TECHNICAL" : "BILLING";
            context.addMessage(ConversationMessage.assistant("Answer " + i, agentType));
        }

        assertEquals(100, context.getMessages().size());

        List<ConversationMessage> recent = context.getRecentMessages(4);
        assertEquals(4, recent.size());
        // Last agent should be BILLING (i=49, 49%2=1, so BILLING)
        assertEquals("BILLING", context.getCurrentAgentType());
    }
}

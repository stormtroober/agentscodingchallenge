package org.example.agent;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test per AgentType enum.
 * Verifica le definizioni corrette degli agenti.
 */
public class AgentTypeTest {

    @Test
    public void testAgentTypesExist() {
        assertNotNull(AgentType.TECHNICAL);
        assertNotNull(AgentType.BILLING);
        assertNotNull(AgentType.UNKNOWN);
    }

    @Test
    public void testTechnicalAgentDescription() {
        String description = AgentType.TECHNICAL.getDescription();
        assertNotNull(description);
        assertTrue("Should mention Technical", description.contains("Technical"));
        assertTrue("Should mention documentation", description.toLowerCase().contains("documentation"));
    }

    @Test
    public void testBillingAgentDescription() {
        String description = AgentType.BILLING.getDescription();
        assertNotNull(description);
        assertTrue("Should mention Billing", description.contains("Billing"));
        assertTrue("Should mention refunds", description.toLowerCase().contains("refund"));
    }

    @Test
    public void testUnknownAgentDescription() {
        String description = AgentType.UNKNOWN.getDescription();
        assertNotNull(description);
        assertTrue("Should mention outside expertise",
                description.toLowerCase().contains("outside") ||
                        description.toLowerCase().contains("unknown"));
    }

    @Test
    public void testAgentTypeValues() {
        AgentType[] values = AgentType.values();
        assertEquals(3, values.length);
    }

    @Test
    public void testAgentTypeValueOf() {
        assertEquals(AgentType.TECHNICAL, AgentType.valueOf("TECHNICAL"));
        assertEquals(AgentType.BILLING, AgentType.valueOf("BILLING"));
        assertEquals(AgentType.UNKNOWN, AgentType.valueOf("UNKNOWN"));
    }

    @Test
    public void testAgentTypeName() {
        assertEquals("TECHNICAL", AgentType.TECHNICAL.name());
        assertEquals("BILLING", AgentType.BILLING.name());
        assertEquals("UNKNOWN", AgentType.UNKNOWN.name());
    }

    @Test
    public void testAgentTypeOrdinal() {
        assertEquals(0, AgentType.TECHNICAL.ordinal());
        assertEquals(1, AgentType.BILLING.ordinal());
        assertEquals(2, AgentType.UNKNOWN.ordinal());
    }
}

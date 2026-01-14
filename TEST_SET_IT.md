# Test Set - Multi-Agent Support System

This document contains test cases for validating the multi-agent support system with Technical and Billing specialists.

## Requirements Coverage

| Requirement | Agent | Tools/Docs |
|-------------|-------|------------|
| 3-4 documentation sources | Technical | troubleshooting.md, integration_guide.md, faq.md, system_requirements.md, installation.md |
| 3+ billing capabilities | Billing | open_refund_case, get_refund_timeline, search_billing_policy |
| Dynamic agent switching | Coordinator | Routes based on intent |
| Multi-turn conversations | Both | Context maintained |
| Graceful handling | Both | Out-of-scope responses |

---

## Category 1: Billing Specialist - Core Capabilities

### Capability 1: Open Refund Case

#### TEST 1.1 - Refund request with complete data
**Input:**
```
Vorrei un rimborso. Email: test@example.com. Motivo: servizio lento
```
**Expected Behavior:**
- Agent calls `open_refund_case` tool
- Returns Case ID (REF-XXXX)
- Provides the direct link to the refund form
- Explains next steps (7 business days to complete form)

#### TEST 1.2 - Refund request multi-turn (missing info)
**Turn 1 Input:**
```
Voglio un rimborso
```
**Expected:** Agent asks for email/customer ID

**Turn 2 Input:**
```
cliente@mail.com
```
**Expected:** Agent asks for reason

**Turn 3 Input:**
```
Non uso più il servizio
```
**Expected:** Case opened with REF-XXXX, form link provided

---

### Capability 2: Check Plan/Price

#### TEST 1.3 - General Plan Information
**Input:**
```
Quali sono i piani disponibili?
```
**Expected Behavior:**
- Agent calls `search_billing_policy` tool
- Returns generic plan details: Basic, Professional, Enterprise from policy

#### TEST 1.4 - Generic price question
**Input:**
```
Quanto costa il piano Professional?
```
**Expected:** Agent uses `search_billing_policy` tool to find pricing information

---

### Capability 3: Refund Timelines

#### TEST 1.5 - Full refund timeline
**Input:**
```
Quanto ci vuole per un rimborso totale? Ho piano Basic mensile
```
**Expected Behavior:**
- Agent calls `get_refund_timeline(refund_type=full, plan_type=basic)`
- Returns: 5-7 business days
- Explains eligibility: within 14 days for monthly plans

#### TEST 1.6 - Enterprise expedited refund
**Input:**
```
Tempi di rimborso per piano Enterprise?
```
**Expected:** 2-3 business days (expedited processing)

#### TEST 1.7 - Prorated refund on downgrade
**Input:**
```
Voglio fare downgrade, avrò un rimborso?
```
**Expected Behavior:**
- Agent calls `get_refund_timeline(refund_type=prorated)`
- Returns: Applied as account credit by default
- Explains prorated calculation

---

### Capability 4: Billing Policy Search

#### TEST 1.8 - Cancellation policy
**Input:**
```
Come posso cancellare il mio abbonamento?
```
**Expected Behavior:**
- Agent calls `search_billing_policy(query=cancellation)`
- Returns steps: Settings → Subscription → Cancel
- Explains data retention (30 days)

#### TEST 1.9 - Payment methods
**Input:**
```
Quali metodi di pagamento accettate?
```
**Expected:** Credit/Debit cards, PayPal, Bank transfer (Enterprise only)

---

## Category 2: Technical Specialist - Documentation Sources

### Doc 1: Troubleshooting Guide

#### TEST 2.1 - Connection timeout
**Input:**
```
Ho errore CONN_TIMEOUT, cosa faccio?
```
**Expected Behavior:**
- Agent calls `search_documentation(query=connection timeout)`
- Returns 5 troubleshooting steps
- Lists error codes: CONN_TIMEOUT, CONN_REFUSED, CONN_RESET

#### TEST 2.2 - Invalid API Key
**Input:**
```
L'API dice Invalid API Key
```
**Expected:** 4 solutions from troubleshooting guide

#### TEST 2.3 - Rate limiting error
**Input:**
```
Sto ricevendo errore 429
```
**Expected:** Explanation of rate limiting + suggestions to reduce frequency

---

### Doc 2: Integration Guide

#### TEST 2.4 - API authentication
**Input:**
```
Come configuro l'autenticazione API?
```
**Expected Behavior:**
- Returns info on API Key + Authorization header
- Security best practices

#### TEST 2.5 - Webhook setup
**Input:**
```
Come imposto i webhook?
```
**Expected:** 4 steps from integration_guide.md

#### TEST 2.6 - OAuth flow
**Input:**
```
Come funziona OAuth 2.0 nel vostro sistema?
```
**Expected:** OAuth flow steps: register app, redirect, callback, token exchange

---

### Doc 3: System Requirements

#### TEST 2.7 - Java SDK requirements
**Input:**
```
Cosa serve per l'SDK Java?
```
**Expected:** Java 11+, Maven 3.6+ or Gradle 7.0+, 256MB RAM

#### TEST 2.8 - Self-hosted requirements
**Input:**
```
Requisiti per deployment self-hosted?
```
**Expected:** 2 CPU cores, 4GB RAM, 20GB SSD, OS requirements

#### TEST 2.9 - Browser compatibility
**Input:**
```
Quali browser supportate?
```
**Expected:** Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

---

### Doc 4: Installation Guide

#### TEST 2.10 - Linux installation
**Input:**
```
Come installo su Ubuntu?
```
**Expected:** wget, tar, install.sh, systemctl commands

#### TEST 2.11 - Python SDK installation
**Input:**
```
Come installo l'SDK Python?
```
**Expected:** `pip install example-sdk`

---

## Category 3: Agent Routing (Dynamic Switching)

#### TEST 3.1 - Route to Billing
**Input:**
```
I want a refund
```
**Expected:** BILLING agent handles the request (asks for details or provides refund assistance)

#### TEST 3.2 - Route to Technical
**Input:**
```
I get error 500 from API
```
**Expected:** TECHNICAL agent handles the request (provides troubleshooting help for 500 errors)

#### TEST 3.3 - Switch mid-conversation
**Turn 1 Input:**
```
Voglio un rimborso
```
**Expected:** BILLING handles

**Turn 2 Input:**
```
Perfetto, ho anche un problema tecnico con l'API
```
**Expected:** Either explicit transfer to TECHNICAL or clear indication that technical support will help

#### TEST 3.4 - Ambiguous request
**Input:**
```
Il mio account non funziona
```
**Expected:** Agent asks for clarification OR makes reasonable routing decision

---

## Category 4: Edge Cases (Graceful Handling)

#### TEST 4.1 - Completely out of scope
**Input:**
```
Qual è la capitale del Giappone?
```
**Expected Behavior:**
- Agent acknowledges this is outside their expertise
- Redirects to relevant topics (billing or technical support)

#### TEST 4.2 - Documentation not available
**Input:**
```
Come integro con Kubernetes?
```
**Expected Behavior:**
- Technical agent admits documentation doesn't cover this
- Offers to escalate or asks for more details

#### TEST 4.3 - Unreasonable billing request
**Input:**
```
Voglio 10 volte il rimborso dovuto
```
**Expected:** Explains refund policy, does not accept fraudulent request

---

## Category 5: Multi-Turn Conversations

#### TEST 5.1 - Multi-step billing conversation
| Turn | Input | Expected |
|------|-------|----------|
| 1 | "Vorrei un rimborso" | Agent asks for email/customer ID |
| 2 | "mario@test.it" | Agent asks for reason |
| 3 | "Troppo costoso" | Case opened |
| 4 | "Quanto dovrò aspettare?" | Timeline info with context retained |

#### TEST 5.2 - Technical follow-up
| Turn | Input | Expected |
|------|-------|----------|
| 1 | "Ho problemi di connessione" | Troubleshooting steps OR ask for specific details about the connection issue |
| 2 | "E se il problema persiste?" | Next steps / escalation option |

#### TEST 5.3 - Context retention
| Turn | Input | Expected |
|------|-------|----------|
| 1 | "Sono cliente Enterprise" | Acknowledged |
| 2 | "Quanto ci vuole per un rimborso?" | Response considers Enterprise context → 2-3 days expedited |

---

## Test Execution Checklist

### Billing Agent Tests
- [x] TEST 1.1 - Refund with complete data
- [x] TEST 1.2 - Refund multi-turn
- [x] TEST 1.3 - General Plan Information
- [x] TEST 1.4 - Generic price question
- [x] TEST 1.5 - Full refund timeline
- [x] TEST 1.6 - Enterprise expedited
- [x] TEST 1.7 - Prorated refund
- [x] TEST 1.8 - Cancellation policy
- [x] TEST 1.9 - Payment methods

### Technical Agent Tests
- [x] TEST 2.1 - Connection timeout
- [x] TEST 2.2 - Invalid API Key
- [x] TEST 2.3 - Rate limiting
- [x] TEST 2.4 - API authentication
- [x] TEST 2.5 - Webhook setup
- [x] TEST 2.6 - OAuth flow
- [x] TEST 2.7 - Java SDK requirements
- [x] TEST 2.8 - Self-hosted requirements
- [x] TEST 2.9 - Browser compatibility
- [x] TEST 2.10 - Linux installation
- [x] TEST 2.11 - Python SDK

### Routing Tests
- [x] TEST 3.1 - Route to Billing
- [x] TEST 3.2 - Route to Technical
- [x] TEST 3.3 - Switch mid-conversation
- [x] TEST 3.4 - Ambiguous request

### Edge Case Tests
- [x] TEST 4.1 - Out of scope
- [x] TEST 4.2 - Missing documentation
- [x] TEST 4.3 - Unreasonable request

### Multi-Turn Tests
- [x] TEST 5.1 - Multi-step billing
- [x] TEST 5.2 - Technical follow-up
- [x] TEST 5.3 - Context retention

---

## Summary

| Category | Total Tests |
|----------|-------------|
| Billing Capabilities | 9 |
| Technical Documentation | 11 |
| Agent Routing | 4 |
| Edge Cases | 3 |
| Multi-Turn | 3 |
| **TOTAL** | **30** |

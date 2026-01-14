# Test Set - Multi-Agent Support System (Polski)

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
Chciałbym zwrot pieniędzy. Email: test@example.com. Powód: wolna usługa
```
**Expected Behavior:**
- Agent calls `open_refund_case` tool
- Returns Case ID (REF-XXXX)
- Provides the direct link to the refund form
- Explains next steps (7 business days to complete form)

#### TEST 1.2 - Refund request multi-turn (missing info)
**Turn 1 Input:**
```
Chcę zwrot pieniędzy
```
**Expected:** Agent asks for email/customer ID

**Turn 2 Input:**
```
klient@mail.com
```
**Expected:** Agent asks for reason

**Turn 3 Input:**
```
Nie korzystam już z usługi
```
**Expected:** Case opened with REF-XXXX, form link provided

---

### Capability 2: Check Plan/Price

#### TEST 1.3 - General Plan Information
**Input:**
```
Jakie są dostępne plany?
```
**Expected Behavior:**
- Agent calls `search_billing_policy` tool
- Returns generic plan details: Basic, Professional, Enterprise from policy

#### TEST 1.4 - Generic price question
**Input:**
```
Ile kosztuje plan Professional?
```
**Expected:** Agent uses `search_billing_policy` tool to find pricing information

---

### Capability 3: Refund Timelines

#### TEST 1.5 - Full refund timeline
**Input:**
```
Ile trwa pełny zwrot pieniędzy? Mam miesięczny plan Basic
```
**Expected Behavior:**
- Agent calls `get_refund_timeline(refund_type=full, plan_type=basic)`
- Returns: 5-7 business days
- Explains eligibility: within 14 days for monthly plans

#### TEST 1.6 - Enterprise expedited refund
**Input:**
```
Czas zwrotu dla planu Enterprise?
```
**Expected:** 2-3 business days (expedited processing)

#### TEST 1.7 - Prorated refund on downgrade
**Input:**
```
Chcę obniżyć plan, czy dostanę zwrot?
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
Jak mogę anulować subskrypcję?
```
**Expected Behavior:**
- Agent calls `search_billing_policy(query=cancellation)`
- Returns steps: Settings → Subscription → Cancel
- Explains data retention (30 days)

#### TEST 1.9 - Payment methods
**Input:**
```
Jakie metody płatności akceptujecie?
```
**Expected:** Credit/Debit cards, PayPal, Bank transfer (Enterprise only)

---

## Category 2: Technical Specialist - Documentation Sources

### Doc 1: Troubleshooting Guide

#### TEST 2.1 - Connection timeout
**Input:**
```
Mam błąd CONN_TIMEOUT, co robić?
```
**Expected Behavior:**
- Agent calls `search_documentation(query=connection timeout)`
- Returns troubleshooting steps for connection issues

#### TEST 2.2 - Invalid API Key
**Input:**
```
API pokazuje Invalid API Key
```
**Expected:** 4 solutions from troubleshooting guide

#### TEST 2.3 - Rate limiting error
**Input:**
```
Otrzymuję błąd 429
```
**Expected:** Explanation of rate limiting + suggestions to reduce frequency

---

### Doc 2: Integration Guide

#### TEST 2.4 - API authentication
**Input:**
```
Jak skonfigurować uwierzytelnianie API?
```
**Expected Behavior:**
- Returns info on API Key + Authorization header
- Security best practices

#### TEST 2.5 - Webhook setup
**Input:**
```
Jak ustawić webhooki?
```
**Expected:** 4 steps from integration_guide.md

#### TEST 2.6 - OAuth flow
**Input:**
```
Jak działa OAuth 2.0 w waszym systemie?
```
**Expected:** OAuth flow steps: register app, redirect, callback, token exchange

---

### Doc 3: System Requirements

#### TEST 2.7 - Java SDK requirements
**Input:**
```
Co jest potrzebne do SDK Java?
```
**Expected:** Java 11+, Maven 3.6+ or Gradle 7.0+, 256MB RAM

#### TEST 2.8 - Self-hosted requirements
**Input:**
```
Wymagania dla wdrożenia self-hosted?
```
**Expected:** 2 CPU cores, 4GB RAM, 20GB SSD, OS requirements

#### TEST 2.9 - Browser compatibility
**Input:**
```
Jakie przeglądarki obsługujecie?
```
**Expected:** Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

---

### Doc 4: Installation Guide

#### TEST 2.10 - Linux installation
**Input:**
```
Jak zainstalować na Ubuntu?
```
**Expected:** wget, tar, install.sh, systemctl commands

#### TEST 2.11 - Python SDK installation
**Input:**
```
Jak zainstalować SDK Python?
```
**Expected:** `pip install example-sdk`

---

## Category 3: Agent Routing (Dynamic Switching)

#### TEST 3.1 - Route to Billing
**Input:**
```
Chcę zwrot pieniędzy
```
**Expected:** BILLING agent handles the request (asks for details or provides refund assistance)

#### TEST 3.2 - Route to Technical
**Input:**
```
Dostaję błąd 500 z API
```
**Expected:** TECHNICAL agent handles the request (provides troubleshooting help for 500 errors)

#### TEST 3.3 - Switch mid-conversation
**Turn 1 Input:**
```
Chcę zwrot pieniędzy
```
**Expected:** BILLING handles

**Turn 2 Input:**
```
Świetnie, mam też problem techniczny z API
```
**Expected:** Either explicit transfer to TECHNICAL or clear indication that technical support will help

#### TEST 3.4 - Ambiguous request
**Input:**
```
Moje konto nie działa
```
**Expected:** Agent asks for clarification OR makes reasonable routing decision

---

## Category 4: Edge Cases (Graceful Handling)

#### TEST 4.1 - Completely out of scope
**Input:**
```
Jaka jest stolica Japonii?
```
**Expected Behavior:**
- Agent acknowledges this is outside their expertise
- Redirects to relevant topics (billing or technical support)

#### TEST 4.2 - Documentation not available
**Input:**
```
Jak zintegrować z Kubernetes?
```
**Expected Behavior:**
- Technical agent admits documentation doesn't cover this
- Offers to escalate or asks for more details

#### TEST 4.3 - Unreasonable billing request
**Input:**
```
Chcę 10 razy więcej zwrotu niż mi się należy
```
**Expected:** Explains refund policy, does not accept fraudulent request

---

## Category 5: Multi-Turn Conversations

#### TEST 5.1 - Multi-step billing conversation
| Turn | Input | Expected |
|------|-------|----------|
| 1 | "Chciałbym zwrot pieniędzy" | Agent asks for email/customer ID |
| 2 | "jan@test.pl" | Agent asks for reason |
| 3 | "Za drogie" | Case opened |
| 4 | "Ile będę musiał czekać?" | Refund timeline (5-7 days or similar) |

#### TEST 5.2 - Technical follow-up
| Turn | Input | Expected |
|------|-------|----------|
| 1 | "Mam problemy z połączeniem" | Troubleshooting steps |
| 2 | "A jeśli problem będzie się powtarzał?" | Next steps / escalation option |

#### TEST 5.3 - Context retention
| Turn | Input | Expected |
|------|-------|----------|
| 1 | "Jestem klientem Enterprise" | Acknowledged |
| 2 | "Ile trwa zwrot pieniędzy?" | Response considers Enterprise context → 2-3 days expedited |

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

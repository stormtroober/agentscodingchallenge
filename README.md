# Multi-Agent Conversational Support System

A Java-based support chat application featuring specialized agents for Technical Support and Billing inquiries, orchestrated by a Coordinator agent.

## Features
- **Coordinator Agent**: Intelligently routes queries to the correct specialist.
- **Technical Specialist**: Answers questions based *strictly* on local documentation.
- **Billing Specialist**: Handles refund requests, plan checks, and policy inquiries using tool calling.
- **CLI Interface**: Simple interactive command-line interface.

## Setup

1. **Prerequisites**:
   - Java 21+
   - Gradle (wrapper included)
   - A Gemini API Key

2. **Configuration**:
   Create a `.env` file in the project root:
   ```bash
   GEMINI_API_KEY=your_api_key_here
   ```

## Running

Run the application using Gradle:

```bash
./gradlew run --console=plain
```

## Usage
- **Technical**: Ask about connection timeouts, API integration, or system requirements.
- **Billing**: Ask for a refund, check your plan, or ask about billing policies.
- **Control**: Type `clear` to reset context or `quit` to exit.

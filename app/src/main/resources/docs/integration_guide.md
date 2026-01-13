# Integration Guide

## Getting Started

### Prerequisites
Before integrating, ensure you have:

- An active account with API access enabled
- Your API key from the dashboard
- Java 11+ or Node.js 16+ (depending on your stack)

### Quick Start

1. **Install the SDK** (optional but recommended):
   ```bash
   # For Java (Maven)
   mvn install our-sdk:1.0.0
   
   # For Node.js
   npm install our-sdk
   ```

2. **Set up authentication**:
   ```java
   // Java example
   ApiClient client = new ApiClient("your-api-key");
   ```

3. **Make your first request**:
   ```java
   Response response = client.getData("/endpoint");
   ```

## Authentication Setup

### API Key Authentication
The simplest method - include your API key in the header:

```
Authorization: Bearer YOUR_API_KEY
```

**Security best practices:**
- Never commit API keys to version control
- Use environment variables for storing keys
- Rotate keys periodically
- Use different keys for staging/production

### OAuth 2.0 Integration
For applications accessing user data:

1. Register your application in the dashboard
2. Implement the OAuth flow:
   - Redirect user to authorization URL
   - Handle callback with authorization code
   - Exchange code for access token
3. Use access token for API requests
4. Implement token refresh handling

## API Endpoints

### Base URLs
- Production: `https://api.example.com/v1`
- Staging: `https://staging-api.example.com/v1`

### Common Headers
All requests should include:
- `Content-Type: application/json`
- `Authorization: Bearer YOUR_TOKEN`
- `X-Request-ID: unique-request-id` (optional, for tracking)

## Webhooks

### Setting Up Webhooks

1. Go to Dashboard → Settings → Webhooks
2. Add your endpoint URL (must be HTTPS)
3. Select events to subscribe to
4. Save and note the webhook secret

### Verifying Webhook Signatures
Always verify the signature header to ensure authenticity:

```java
String signature = request.getHeader("X-Signature");
boolean isValid = WebhookUtil.verifySignature(
    requestBody, 
    signature, 
    webhookSecret
);
```

### Webhook Events
- `data.created` - New data was created
- `data.updated` - Existing data was modified
- `data.deleted` - Data was removed
- `payment.completed` - Payment was processed
- `subscription.changed` - Subscription status changed

## Best Practices

### Error Handling
Implement robust error handling:

1. Check HTTP status codes
2. Parse error response bodies for details
3. Log errors with request IDs for debugging
4. Implement retry logic for transient failures

### Rate Limiting
Our API has the following limits:

| Plan | Requests/minute | Requests/day |
|------|-----------------|--------------|
| Basic | 60 | 10,000 |
| Professional | 300 | 100,000 |
| Enterprise | 1,000 | Unlimited |

### Pagination
For endpoints returning lists:

```
GET /items?page=1&limit=50
```

Response includes pagination info:
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 50,
    "total": 1234,
    "hasMore": true
  }
}
```

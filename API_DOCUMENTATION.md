# API Documentation

## Overview

The LLM Conversational Agent provides a RESTful API for managing conversations with AI language models. This API supports multi-turn conversations, health monitoring, and conversation analytics.

**Base URL:** `http://localhost:8080`

**API Version:** v1

---

## Authentication

Currently, the API accepts requests without authentication. API key authentication is planned for future releases.

---

## Endpoints

### 1. Root Endpoint

**GET** `/`

Returns basic service information.

**Response (200 OK):**
```json
{
  "name": "LLM Conversational Agent",
  "version": "1.0.0",
  "status": "running"
}
```

---

### 2. Health Check

**GET** `/health`

Checks the health status of the service and LLM provider.

**Response (200 OK):**
```json
{
  "status": "healthy",
  "message": "LLM Conversational Agent is running"
}
```

**Response (503 Service Unavailable):**
```json
{
  "status": "unhealthy",
  "message": "Health check failed: <error message>"
}
```

---

### 3. Send Chat Message

**POST** `/api/v1/chat`

Send a message to the conversational agent and receive a response.

**Request Body:**
```json
{
  "message": "Your message here",
  "conversationId": "unique-conversation-id"
}
```

**Parameters:**
- `message` (string, required): The user's message (must not be empty)
- `conversationId` (string, required): Unique identifier for the conversation thread

**Response (200 OK):**
```json
{
  "response": "AI assistant's response",
  "conversationId": "unique-conversation-id",
  "timestamp": "2026-03-06T12:34:56.789Z"
}
```

**Response (400 Bad Request):**
```json
{
  "error": "Message cannot be empty"
}
```

**Response (500 Internal Server Error):**
```json
{
  "error": "Error processing request: <error details>"
}
```

---

### 4. Get Statistics

**GET** `/api/v1/stats`

Retrieve conversation statistics and system metrics.

**Response (200 OK):**
```json
{
  "activeConversations": 5,
  "totalConversations": 150,
  "totalMessages": 1247
}
```

---

### 5. Get Conversation History

**GET** `/api/v1/conversation/{conversationId}`

Retrieve the full history of a specific conversation.

**Path Parameters:**
- `conversationId` (string, required): The conversation identifier

**Response (200 OK):**
```json
{
  "conversationId": "unique-conversation-id",
  "messagesCount": 10,
  "messages": [
    {
      "role": "user",
      "content": "Hello, what is AI?"
    },
    {
      "role": "assistant",
      "content": "AI stands for Artificial Intelligence..."
    }
  ]
}
```

**Response (404 Not Found):**
```json
{
  "error": "Conversation not found"
}
```

---

## Error Handling

All errors follow a consistent JSON format:

```json
{
  "error": "Description of the error"
}
```

### HTTP Status Codes

- `200 OK`: Request successful
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server-side error
- `503 Service Unavailable`: Service temporarily unavailable

---

## Rate Limiting

Currently, no rate limiting is enforced. Future versions will implement rate limiting to ensure fair usage.

---

## Examples

### Example 1: Start a New Conversation

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, can you explain machine learning?",
    "conversationId": "user-123"
  }'
```

### Example 2: Continue a Conversation

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What are neural networks?",
    "conversationId": "user-123"
  }'
```

### Example 3: Check Service Health

```bash
curl http://localhost:8080/health
```

### Example 4: Get Conversation History

```bash
curl http://localhost:8080/api/v1/conversation/user-123
```

---

## Best Practices

1. **Unique Conversation IDs**: Use unique identifiers for each conversation thread to maintain context separation
2. **Error Handling**: Always check HTTP status codes and handle errors appropriately
3. **Message Validation**: Ensure messages are not empty before sending
4. **Monitoring**: Regularly check the `/health` endpoint for service status

---

## Support

For issues, feature requests, or questions:
- GitHub: https://github.com/Harshjain5903/LLM-Conversational-Agent
- Email: harsh.jain@example.com

---

**Last Updated:** March 6, 2026

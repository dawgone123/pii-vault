# PII Vault API Documentation

This document provides comprehensive documentation for the PII Vault encryption/decryption REST API.

## Overview

PII Vault is an immutable, token-based encryption service. The core principles are:
- **Encryption**: Send PII data → Receive opaque token (immutably bound to token_id)
- **Decryption**: Send opaque token → Receive PII data
- **Immutability**: All encrypted PII entries are immutable; cannot be updated or deleted
- **Token Binding**: Each encrypted PII entry is cryptographically bound to a unique token_id

All operations use UUID v7 for request correlation and tracing.

## Base URL

```
http://localhost:8080/api
```

## Quick Reference - All Endpoints

| Endpoint | Method | Purpose | Phase |
|----------|--------|---------|-------|
| `/health` | GET | Health check (Spring Actuator) | ✅ Phase 1 |
| `/metrics` | GET | List available metrics | ✅ Phase 1 |
| `/prometheus` | GET | Prometheus format metrics | ✅ Phase 1 |
| `/v1/encrypt` | POST | Encrypt single PII entry | ✅ Phase 1 |
| `/v1/decrypt` | POST | Decrypt token to PII | ✅ Phase 1 |
| `/v1/batch/encrypt` | POST | Encrypt multiple entries | 📋 Phase 2 |
| `/v1/batch/decrypt` | POST | Decrypt multiple tokens | 📋 Phase 2 |
| `/v1/admin/tokens/{id}` | DELETE | Soft-delete token | 📋 Phase 2 |
| `/v1/admin/tokens/{id}/events` | GET | Get token lifecycle events | 📋 Phase 2 |
| `/v1/admin/events/stream` | GET | Stream token events (NDJSON) | 📋 Phase 2 |

**Full URLs:**
```
GET  http://localhost:8080/api/health
GET  http://localhost:8080/api/metrics
GET  http://localhost:8080/api/prometheus
POST http://localhost:8080/api/v1/encrypt
POST http://localhost:8080/api/v1/decrypt
```

All business logic endpoints are under `/v1/` prefix

## Authentication

Authentication will be implemented in Phase 2. Currently available for development.

Phase 2 will include:
```
Authorization: Bearer <JWT_TOKEN>
```

## Request/Response Formats

### Common Request Headers
```
Content-Type: application/json
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000  (Optional, UUID v7)
```

### Success Response (200 OK)

#### Single Operation Response
```json
{
  "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0...",
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "piiType": "email",
  "encryptedAt": "2026-07-07T08:00:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Batch Operation Response
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440003",
  "processedAt": "2026-07-07T08:10:00Z",
  "successCount": 2,
  "failureCount": 0,
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440004",
      "success": true,
      "token": "...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440006"
    }
  ]
}
```

### Error Response

```json
{
  "error": "INVALID_PII",
  "message": "PII value cannot be empty",
  "status": 400,
  "timestamp": "2026-07-07T08:00:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Endpoints

### Single Operations

#### 1. Encrypt PII Data

Encrypt a single PII entry and receive an opaque token.

**Endpoint:** `POST /v1/encrypt`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "piiType": "email",
  "piiValue": "user@example.com",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIiwiZXlK...",
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "piiType": "email",
  "encryptedAt": "2026-07-07T08:00:00Z",
  "expiresAt": null,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Request Parameters:**
- `piiType` (required): Type of PII (email, phone, ssn, credit_card, etc.)
- `piiValue` (required): The actual PII value to encrypt
- `requestId` (optional): UUID v7 for request tracking

**Response Fields:**
- `token`: Encrypted token representing the PII (base64 encoded)
- `tokenId`: Unique immutable identifier for this encrypted PII entry (UUID v7)
- `piiType`: The type of PII encrypted
- `encryptedAt`: ISO 8601 timestamp when encryption occurred (immutable)
- `expiresAt`: Token expiration time (null if no expiration)
- `requestId`: Echo of the request ID

**Immutability Guarantee:**
Once a PII entry is successfully encrypted:
- The `tokenId` is permanently bound to the encrypted data
- The encrypted entry is immutable - cannot be updated or modified
- The entry can only be decrypted or invalidated (via key rotation)
- Same PII input always produces the same `tokenId` (deterministic encryption)
- To update PII, you must re-encrypt with a new PII value to get a new `tokenId`

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiType": "email",
    "piiValue": "john.doe@example.com",
    "requestId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

**Example using Python:**
```python
import requests
import json

url = "http://localhost:8080/api/v1/encrypt"
payload = {
    "piiType": "email",
    "piiValue": "john.doe@example.com",
    "requestId": "550e8400-e29b-41d4-a716-446655440000"
}

response = requests.post(url, json=payload)
result = response.json()
token = result['token']
print(f"Token: {token}")
```

---

#### 2. Decrypt Token to PII

Decrypt an opaque token and receive the original PII data.

**Endpoint:** `POST /v1/decrypt`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIiwiZXlK...",
  "requestId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response (200 OK):**
```json
{
  "piiType": "email",
  "piiValue": "john.doe@example.com",
  "decryptedAt": "2026-07-07T08:05:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Request Parameters:**
- `token` (required): The encrypted token to decrypt
- `requestId` (optional): UUID v7 for request tracking

**Response Fields:**
- `piiType`: The type of PII decrypted
- `piiValue`: The decrypted PII value
- `decryptedAt`: ISO 8601 timestamp when decryption occurred
- `requestId`: Echo of the request ID

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/decrypt \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIiwiZXlK...",
    "requestId": "550e8400-e29b-41d4-a716-446655440002"
  }'
```

---

### Batch Operations

#### 3. Batch Encrypt PII Data

Encrypt multiple PII entries in a single request. Each entry is mapped to a UUID v7.

**Endpoint:** `POST /v1/batch/encrypt`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440003",
  "entries": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440004",
      "piiType": "email",
      "piiValue": "user1@example.com"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440005",
      "piiType": "phone",
      "piiValue": "+1-555-123-4567"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440006",
      "piiType": "ssn",
      "piiValue": "123-45-6789"
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440003",
  "encryptedAt": "2026-07-07T08:10:00Z",
  "successCount": 3,
  "failureCount": 0,
  "totalProcessed": 3,
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440004",
      "success": true,
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440007",
      "piiType": "email"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440005",
      "success": true,
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6InBob25lIn0...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440008",
      "piiType": "phone"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440006",
      "success": true,
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6InNzbiIn0...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440009",
      "piiType": "ssn"
    }
  ]
}
```

**Request Parameters:**
- `requestId` (optional): UUID v7 for request tracking
- `entries[]` (required): Array of PII entries to encrypt
  - `uuid` (required): UUID v7 for correlating this entry in responses
  - `piiType` (required): Type of PII
  - `piiValue` (required): The PII value to encrypt

**Response Fields:**
- `requestId`: Echo of the request ID
- `encryptedAt`: When encryption completed
- `successCount`: Number of successful encryptions
- `failureCount`: Number of failed encryptions
- `totalProcessed`: Total entries processed
- `results[]`: Array of results (in same order as request)
  - `uuid`: The UUID from the request
  - `success`: Boolean indicating success/failure
  - `token`: Encrypted token (if successful)
  - `tokenId`: Unique token identifier (if successful)
  - `piiType`: The PII type
  - `error`: Error message (if failed)

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/batch/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "550e8400-e29b-41d4-a716-446655440003",
    "entries": [
      {
        "uuid": "550e8400-e29b-41d4-a716-446655440004",
        "piiType": "email",
        "piiValue": "user1@example.com"
      },
      {
        "uuid": "550e8400-e29b-41d4-a716-446655440005",
        "piiType": "phone",
        "piiValue": "+1-555-123-4567"
      }
    ]
  }'
```

**Constraints:**
- Maximum 1,000 entries per request
- Each PII value limited to 10KB
- Request timeout: 30 seconds

---

#### 4. Batch Decrypt Tokens

Decrypt multiple tokens in a single request. Each token is mapped to a UUID v7.

**Endpoint:** `POST /v1/batch/decrypt`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440010",
  "entries": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440011",
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0..."
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440012",
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6InBob25lIn0..."
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440010",
  "decryptedAt": "2026-07-07T08:15:00Z",
  "successCount": 2,
  "failureCount": 0,
  "totalProcessed": 2,
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440011",
      "success": true,
      "piiType": "email",
      "piiValue": "user1@example.com"
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440012",
      "success": true,
      "piiType": "phone",
      "piiValue": "+1-555-123-4567"
    }
  ]
}
```

**Request Parameters:**
- `requestId` (optional): UUID v7 for request tracking
- `entries[]` (required): Array of tokens to decrypt
  - `uuid` (required): UUID v7 for correlating this entry in responses
  - `token` (required): The encrypted token to decrypt

**Response Fields:**
- `requestId`: Echo of the request ID
- `decryptedAt`: When decryption completed
- `successCount`: Number of successful decryptions
- `failureCount`: Number of failed decryptions
- `totalProcessed`: Total entries processed
- `results[]`: Array of results (in same order as request)
  - `uuid`: The UUID from the request
  - `success`: Boolean indicating success/failure
  - `piiType`: The PII type
  - `piiValue`: The decrypted PII value (if successful)
  - `error`: Error message (if failed)

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/batch/decrypt \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "550e8400-e29b-41d4-a716-446655440010",
    "entries": [
      {
        "uuid": "550e8400-e29b-41d4-a716-446655440011",
        "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0..."
      }
    ]
  }'
```

**Constraints:**
- Maximum 1,000 entries per request
- Request timeout: 30 seconds

---

### Admin Operations

#### 5. Soft-Delete Token (Admin Only)

Mark a token as deleted while preserving the encrypted PII data. This enables GDPR right-to-be-forgotten compliance while maintaining audit trails and recovery options.

**Endpoint:** `DELETE /v1/admin/tokens/{tokenId}`

**Authentication:** `Bearer {admin-token}` (Phase 2)

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer {admin-token}
```

**Request Body:**
```json
{
  "reason": "User requested deletion",
  "requestId": "550e8400-e29b-41d4-a716-446655440011"
}
```

**Response (200 OK):**
```json
{
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "deletedAt": "2026-07-07T08:20:00Z",
  "reason": "User requested deletion",
  "recoverable": true,
  "requestId": "550e8400-e29b-41d4-a716-446655440011"
}
```

**Request Parameters:**
- `tokenId` (path, required): Token ID to soft-delete
- `reason` (body, optional): Reason for deletion (audit trail)
- `requestId` (body, optional): UUID v7 for tracking

**Response Fields:**
- `tokenId`: The token that was marked for deletion
- `deletedAt`: ISO8601 timestamp of deletion
- `reason`: Deletion reason (if provided)
- `recoverable`: Whether encrypted data can be recovered by admins
- `requestId`: Echo of request ID

**Behavior:**
- Token marked as deleted in token_metadata table
- Encrypted PII data remains in database
- Immutable deletion record created
- Cannot be undone
- Supports GDPR right-to-be-forgotten
- Admin-only operation (requires elevated authentication)

**Example using cURL:**
```bash
curl -X DELETE http://localhost:8080/api/v1/admin/tokens/550e8400-e29b-41d4-a716-446655440001 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {admin-token}" \
  -d '{
    "reason": "User requested deletion",
    "requestId": "550e8400-e29b-41d4-a716-446655440011"
  }'
```

---

#### 6. Get Token Lifecycle Events (Admin Only)

Retrieve the complete lifecycle history for a token, including creation, access, expiration, and deletion events.

**Endpoint:** `GET /v1/admin/tokens/{tokenId}/events`

**Authentication:** `Bearer {admin-token}` (Phase 2)

**Request Headers:**
```
Authorization: Bearer {admin-token}
```

**Query Parameters:**
- `tokenId` (path, required): Token ID to fetch events for
- `from` (optional): ISO8601 start timestamp
- `to` (optional): ISO8601 end timestamp
- `eventTypes` (optional): Comma-separated list (TOKEN_CREATED, TOKEN_DELETED, TOKEN_ACCESSED, TOKEN_EXPIRED, TOKEN_INVALIDATED)
- `limit` (optional): Max results to return (default: 100, max: 1000)
- `offset` (optional): Pagination offset (default: 0)

**Response (200 OK):**
```json
{
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "events": [
    {
      "eventId": 12345,
      "eventType": "TOKEN_CREATED",
      "eventTimestamp": "2026-07-07T08:00:00Z",
      "triggeredBy": "user@example.com",
      "requestId": "550e8400-e29b-41d4-a716-446655440000"
    },
    {
      "eventId": 12346,
      "eventType": "TOKEN_ACCESSED",
      "eventTimestamp": "2026-07-07T08:05:00Z",
      "triggeredBy": "user@example.com",
      "requestId": "550e8400-e29b-41d4-a716-446655440002"
    },
    {
      "eventId": 12347,
      "eventType": "TOKEN_DELETED",
      "eventTimestamp": "2026-07-07T08:20:00Z",
      "triggeredBy": "admin@example.com",
      "requestId": "550e8400-e29b-41d4-a716-446655440011"
    }
  ],
  "totalEvents": 3,
  "hasMore": false,
  "pagination": {
    "offset": 0,
    "limit": 100
  }
}
```

**Response Fields:**
- `tokenId`: The requested token ID
- `events[]`: Array of lifecycle events
  - `eventId`: Unique event identifier
  - `eventType`: Type of event (see enum below)
  - `eventTimestamp`: When event occurred (ISO8601)
  - `triggeredBy`: User/system that triggered event
  - `requestId`: Correlation ID for request tracing
- `totalEvents`: Total count of events matching criteria
- `hasMore`: Whether more results are available
- `pagination`: Pagination information

**Event Types:**
- `TOKEN_CREATED`: PII was encrypted and token created
- `TOKEN_ACCESSED`: Token was used for decryption
- `TOKEN_DELETED`: Token was marked for soft deletion
- `TOKEN_EXPIRED`: Token expiration was triggered
- `TOKEN_INVALIDATED`: Token was invalidated via key rotation

**Example using cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/tokens/550e8400-e29b-41d4-a716-446655440001/events?from=2026-07-07T00:00:00Z&to=2026-07-08T00:00:00Z" \
  -H "Authorization: Bearer {admin-token}"
```

---

#### 7. Stream Token Events (Admin Only)

Consume token lifecycle events for data lake replication. Events stream in real-time with at-least-once delivery guarantee.

**Endpoint:** `GET /v1/admin/events/stream`

**Authentication:** `Bearer {admin-token}` (Phase 2)

**Request Headers:**
```
Authorization: Bearer {admin-token}
Accept: application/x-ndjson
```

**Query Parameters:**
- `since` (required): ISO8601 timestamp to start streaming from
- `eventTypes` (optional): Comma-separated list of event types to filter
- `tokenIds` (optional): Comma-separated list of token IDs to filter
- `limit` (optional): Max events per response (default: 1000, max: 10000)
- `pollInterval` (optional): Seconds between polls for new events (default: 5)

**Response (200 OK, Streaming):**
```
Content-Type: application/x-ndjson
Transfer-Encoding: chunked

{"eventId": 12348, "tokenId": "550e8400-e29b-41d4-a716-446655440012", "eventType": "TOKEN_CREATED", "eventTimestamp": "2026-07-07T08:25:00Z", "requestId": "550e8400-e29b-41d4-a716-446655440013", "payload": {"piiType": "email", "tokenId": "550e8400-e29b-41d4-a716-446655440012"}}
{"eventId": 12349, "tokenId": "550e8400-e29b-41d4-a716-446655440014", "eventType": "TOKEN_CREATED", "eventTimestamp": "2026-07-07T08:26:00Z", "requestId": "550e8400-e29b-41d4-a716-446655440015", "payload": {"piiType": "phone", "tokenId": "550e8400-e29b-41d4-a716-446655440014"}}
```

**Response Fields (NDJSON format):**
- `eventId`: Unique event identifier (for idempotent processing)
- `tokenId`: Token that triggered event
- `eventType`: Type of event (TOKEN_CREATED, TOKEN_DELETED, etc.)
- `eventTimestamp`: When event occurred (ISO8601)
- `requestId`: Correlation ID for request tracing
- `payload`: Event-specific data
  - For TOKEN_CREATED: Contains piiType and tokenId
  - For TOKEN_DELETED: Contains deletion reason and timestamp
  - For TOKEN_ACCESSED: Contains timestamp
- `nextEventId`: ID of next event to request (for resumption)

**Streaming Behavior:**
- Server sends events matching criteria
- Connection remains open for continuous streaming
- At-least-once delivery guarantee (consumer must deduplicate)
- Use eventId for idempotent processing
- Reconnection carries forward from last seen event
- Server sends heartbeat every 30 seconds if no events

**Data Lake Integration Pattern:**
```
1. Data Lake starts consumer with since=<last_checkpoint>
2. Receives stream of TOKEN_CREATED events
3. For each new token:
   - Records tokenId and eventId
   - Calls batch/decrypt with new tokenIds
   - Stores encrypted PII in data lake
   - Saves eventId as checkpoint
4. On disconnect: resume from checkpoint
```

**Example using cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/admin/events/stream?since=2026-07-07T00:00:00Z&limit=1000" \
  -H "Authorization: Bearer {admin-token}" \
  -H "Accept: application/x-ndjson" \
  --max-time 300
```

---

## Supported PII Types & JSON Blob Storage

### Data Storage Model

All PII data is stored as **JSON entries in a single encrypted blob** rather than separate database columns. This approach optimizes for KMS cost reduction and downstream query performance.

**Example JSON Blob (encrypted and stored as single unit):**
```json
{
  "email": "john@example.com",
  "phone": "+1-555-123-4567",
  "ssn": "123-45-6789",
  "credit_card": "4111-1111-1111-1111",
  "dob": "1990-01-15",
  "address": "123 Main St, City, State"
}
```

### Supported PII Type Keys

| Type Key | Description | Example |
|----------|-------------|---------|
| `email` | Email address | john@example.com |
| `phone` | Phone number | +1-555-123-4567 |
| `ssn` | Social Security Number | 123-45-6789 |
| `credit_card` | Credit card number | 4111-1111-1111-1111 |
| `passport` | Passport number | A12345678 |
| `drivers_license` | Driver's license | DL123456 |
| `dob` | Date of birth | 1990-01-15 |
| `address` | Home address | 123 Main St, City, State |
| `bank_account` | Bank account number | 0123456789 |
| `custom_*` | Custom PII type | (application-specific) |

### API Request Format

**Single Encrypt with Multiple PII Types:**
```json
POST /v1/encrypt

{
  "piiData": {
    "email": "user@example.com",
    "phone": "+1-555-123-4567",
    "ssn": "123-45-6789"
  },
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Batch Encrypt with Multiple PII Types per Entry:**
```json
POST /v1/batch/encrypt

{
  "requestId": "550e8400-e29b-41d4-a716-446655440003",
  "entries": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440004",
      "piiData": {
        "email": "user1@example.com",
        "phone": "+1-555-123-4567"
      }
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440005",
      "piiData": {
        "email": "user2@example.com",
        "credit_card": "4111-1111-1111-1111",
        "dob": "1990-01-15"
      }
    }
  ]
}
```

### Decryption Response Format

**Single Decrypt:**
```json
{
  "piiData": {
    "email": "user@example.com",
    "phone": "+1-555-123-4567",
    "ssn": "123-45-6789"
  },
  "decryptedAt": "2026-07-07T08:05:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Batch Decrypt:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440008",
  "decryptedAt": "2026-07-07T08:15:00Z",
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440009",
      "piiData": {
        "email": "user1@example.com",
        "phone": "+1-555-123-4567"
      },
      "success": true
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440010",
      "piiData": {
        "email": "user2@example.com",
        "credit_card": "4111-1111-1111-1111",
        "dob": "1990-01-15"
      },
      "success": true
    }
  ]
}
```

### Performance Benefits

| Aspect | Traditional (Per-Column) | JSON Blob (Single Unit) | Benefit |
|--------|----------------------|---------------------|---------|
| **KMS Calls** | 6 decrypt calls | 1 decrypt call | 83% cost reduction |
| **Database Queries** | 6 column lookups | 1 blob fetch | Faster retrieval |
| **Joins** | Requires joins | No joins needed | Downstream efficiency |
| **Flexibility** | Schema changes | Add JSON keys | No DDL migrations |
| **Immutability** | Per-column version | Single unit | Simpler enforcement |
| **Network** | Multiple roundtrips | Single response | Reduced latency |

### Cost Impact Example (1M PII Entries)

**Traditional Approach:**
- 1M tokens × 6 types × $0.015 per decrypt = $90,000/month

**JSON Blob Approach:**
- 1M tokens × 1 decrypt call × $0.015 = $15,000/month
- **Savings: $75,000/month (83% reduction)**

---

## Error Codes

| Code | Status | Description | Example |
|------|--------|-------------|---------|
| INVALID_PII | 400 | Invalid PII data | Empty piiValue |
| INVALID_TOKEN | 400 | Invalid or corrupted token | Malformed token |
| INVALID_PII_TYPE | 400 | Unknown PII type | Unknown type |
| TOKEN_EXPIRED | 401 | Token has expired | Past expiration date |
| DECRYPTION_FAILED | 500 | Decryption failed | Corrupted encrypted data |
| ENCRYPTION_FAILED | 500 | Encryption failed | KMS failure (Phase 2) |
| KMS_FAILURE | 503 | KMS service unavailable | AWS KMS down (Phase 2) |
| RATE_LIMIT_EXCEEDED | 429 | Rate limit exceeded | Too many requests |

---

## Token Characteristics

### Token Format
- **Encoding**: Base64 URL-safe
- **Encryption**: AES-256-GCM (double layer with KMS in Phase 2)
- **Integrity**: Built-in authentication tag in GCM mode
- **Size**: Typically 200-500 bytes (depends on PII length)

### Token Lifecycle
- **Creation**: Issued immediately after successful encryption
- **Storage**: Upstream service responsible for storage and rotation
- **Validation**: Verified during decryption
- **Expiration**: Configurable per deployment (no expiration in Phase 1)
- **Revocation**: Tokens remain valid until KMS key rotation (Phase 3)

---

## Request/Response Mapping with UUID v7

### UUID v7 Requirements
- **Format**: RFC 4122 compliant UUID v7
- **Time-Based**: First 48 bits contain timestamp in milliseconds
- **Sortable**: Can be sorted chronologically
- **Traceable**: Enables correlation across microservices

### Example UUID v7
```
550e8400-e29b-41d4-a716-446655440000
^^^^^^^^^          timestamp component
        ^^^^^^^^  version and variant
                ^^^^^^^^^^^^^^^^^^^^  random/sequential component
```

### Request/Response Correlation
```
Request:
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "entries": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "piiValue": "..."
    }
  ]
}

Response:
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",  ← Same as request
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440001",  ← Same as request
      "token": "...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440002"  ← Generated by vault
    }
  ]
}
```

---

## Immutability Model

### Important: All PII Entries are Immutable

**Core Constraint:** Once a PII entry is encrypted and assigned a `tokenId`, it is immutable and cannot be modified or deleted through the PII Vault API.

### What This Means

| Operation | Allowed? | Notes |
|-----------|----------|-------|
| **Encrypt PII** | ✅ Yes | Creates immutable entry with unique tokenId |
| **Decrypt Token** | ✅ Yes | Retrieves decrypted PII (read-only) |
| **Update PII** | ❌ No | Re-encrypt with new PII to get new tokenId |
| **Delete Entry** | ❌ No | Upstream services manage retention/deletion |
| **Modify Token** | ❌ No | tokenId is immutable and cannot be changed |

### Handling Updates

If you need to update PII data:

```
Old Approach (NOT supported):
1. Encrypt PII → get token
2. Update PII via /update endpoint
3. Continue using same token

Correct Approach (Required):
1. Encrypt original PII → get token1
2. Retrieve/decrypt PII via token1
3. Modify PII in your system
4. Encrypt new PII → get token2 (NEW tokenId)
5. Update your system to use token2
6. Mark token1 for deletion (if desired, handled by upstream)
```

### Why Immutability?

**Security Benefits:**
- Prevents tampering with encrypted data
- Eliminates update attack vectors
- Ensures data integrity throughout lifecycle

**Compliance Benefits:**
- Satisfies non-repudiation requirements
- Enables audit trail integrity
- Meets PCI DSS, GDPR, HIPAA requirements

**Operational Benefits:**
- Simpler data model
- Better performance (no locking)
- Easier auditing and debugging

### Token Binding

Each `tokenId` is permanently bound to:
- The encrypted PII data
- The PII type
- The creation timestamp
- The encryption key version (Phase 2)

This binding is cryptographic and cannot be changed.

---

## Best Practices

### Security
1. Always use HTTPS in production (enforced)
2. Never log token values in application logs
3. Rotate tokens periodically (Phase 2)
4. Implement token expiration (Phase 2)
5. Use secure storage for tokens (your responsibility)
6. Implement request signing (Phase 2)

### Performance
1. Use batch endpoints for multiple PII entries
2. Implement connection pooling
3. Use timeouts for all API calls
4. Cache tokens securely (your responsibility)
5. Implement exponential backoff for retries

### Error Handling
1. Always check `success` field in batch responses
2. Retry failed entries individually
3. Log request IDs for debugging
4. Monitor error rates and patterns
5. Alert on KMS failures (Phase 2)

### Data Privacy
1. Minimize PII exposure in your application
2. Decrypt PII only when necessary
3. Implement audit logging in your service
4. Never store decrypted PII in logs
5. Implement data minimization principles

---

## Phase 2 Additions

Phase 2 (Q4 2026) will add:
- `POST /v1/auth/login` - Authentication endpoint
- `POST /v1/tokens/refresh` - Token refresh (for API tokens)
- `GET /v1/audit/logs` - Audit log retrieval
- `GET /v1/health` - Health check endpoint
- `POST /v1/keys/rotate` - Trigger key rotation

---

## Testing the API

### Using Postman

1. Create a POST request to `http://localhost:8080/api/v1/encrypt`
2. Set body to JSON:
   ```json
   {
     "piiType": "email",
     "piiValue": "test@example.com",
     "requestId": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```
3. Copy the token from response
4. Create a POST request to `http://localhost:8080/api/v1/decrypt`
5. Set body to JSON with the token:
   ```json
   {
     "token": "<paste token here>",
     "requestId": "550e8400-e29b-41d4-a716-446655440001"
   }
   ```
6. Verify PII is returned

---

## API Changelog

### Version 1.0.0 (2026-07-07)

**Initial Release**
- Single encrypt operation
- Single decrypt operation
- Batch encrypt operation with UUID v7 mapping
- Batch decrypt operation with UUID v7 mapping
- AES-256-GCM encryption
- Audit logging

### Version 2.0.0 (Q4 2026 - Planned)

**Phase 2 Features**
- AWS KMS integration for double encryption
- Authentication and authorization
- Token lifecycle management
- Comprehensive audit endpoints
- Rate limiting
- Health check endpoint

---

## Support

For API issues or questions:
1. Check this documentation
2. Review error messages and codes
3. Check application logs with request IDs
4. Open an issue on GitHub
5. Contact the development team

---

**Last Updated**: 2026-07-07
**API Version**: 1.0.0

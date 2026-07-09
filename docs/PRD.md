# PII Vault - Product Requirements Document

## Executive Summary

PII Vault is a secure, enterprise-grade Java Spring Boot application designed as a specialized encryption/decryption service for Personally Identifiable Information (PII). The system provides a centralized, highly secure platform for organizations to encrypt and decrypt sensitive personal data while maintaining PCI DSS v4 compliance. PII Vault operates as a token-based service where upstream applications manage PII data but delegate encryption/decryption operations to this vault, receiving opaque tokens in return.

## Product Vision

To become the industry standard for secure PII encryption services by providing a dedicated, highly specialized platform that allows organizations to confidently manage sensitive personal data through double encryption (application-level AES-256 + KMS-managed key encryption), meeting PCI DSS v4, GDPR, CCPA, and HIPAA requirements.

## Product Goals

1. **Security First**: Implement double encryption - AES-256 at application layer + AWS KMS for key management
2. **Token-Based Architecture**: Encrypt PII and return opaque tokens; decrypt tokens to return PII
3. **Immutability by Design**: All PII entries are immutable; once encrypted, data is cryptographically bound to a token_id
4. **Soft-Delete Capability**: Enable token soft-deletion for user privacy while preserving data for admin audit and recovery
5. **Event-Driven Data Replication**: Stream lifecycle events (token created, token deleted, etc.) to enable data lake replication via CDC and decryption in the PCI-safe zone via batch operations to decrypt these events
6. **Scalability**: Support millions of encryption/decryption operations with sub-100ms response times
7. **Compliance**: Meet PCI DSS v4, GDPR, CCPA, and HIPAA requirements out of the box
8. **Auditability**: Complete audit trail of all encryption/decryption operations
9. **Developer Experience**: Simple, intuitive REST API for easy integration
10. **Separation of Concerns**: PII Vault handles only encryption/decryption; upstream services manage search, indexing, and data organization

## Scope Definition

### What PII Vault Does
- **Encrypt PII**: Accept PII data and return encrypted tokens
- **Decrypt Tokens**: Accept encrypted tokens and return decrypted PII data
- **Batch Operations**: Encrypt/decrypt multiple PII entries with UUID v7 mapping
- **Key Management**: Integration with AWS KMS for secure key management and rotation
- **Audit Logging**: Track all encryption/decryption operations with requestor information
- **Token Management**: Issue and validate opaque tokens that represent encrypted PII
- **Immutable Storage**: Store encrypted PII entries as immutable records bound to token_id
- **Deterministic Encryption**: Same PII input always produces same token_id (when using same encryption key)
- **Soft-Delete Tokens**: Logically delete tokens while preserving encrypted data for admin recovery
- **Event Streaming**: Stream token lifecycle events via CDC to data lake for replication
- **PCI-Safe Decryption**: Enable decryption of events in data lake's PCI-safe zone via batch operations

### What PII Vault Does NOT Do
- **Search Capabilities**: No search functionality - upstream services own all search/query operations
- **Blind Indexes**: No support for encrypted index creation or searchable encryption
- **Data Storage Organization**: No business logic for organizing or categorizing PII
- **Hard Data Deletion**: No permanent deletion of encrypted PII - soft-delete only (with admin recovery option). However, operational tier partitioning allows hard deletion of soft-deleted data after N months (typically 1 year) for compliance and storage optimization
- **Access Control Granularity**: Role-based access is at the service level, not data level
- **Data Retention Management**: Upstream services manage retention policies
- **Event Consumption**: PII Vault streams events but doesn't consume or act on external events
- **Data Lake Management**: PII Vault produces events; downstream systems consume and replicate

## Key Features

### 1. Double Encryption Architecture
- **Application-Level Encryption**: AES-256-GCM encryption at the application layer
- **KMS-Managed Keys**: Integration with AWS KMS for secure key management
- **Key Rotation**: Automated key rotation via KMS
- **Encryption at Rest**: All data in transit and at rest is encrypted

### 2. Token-Based API
- **Encrypt Operation**: Accept PII data, return encrypted token
- **Decrypt Operation**: Accept encrypted token, return decrypted PII
- **Batch Encrypt**: Process multiple PII entries, return tokens mapped to UUIDv7
- **Batch Decrypt**: Process multiple tokens, return PII entries mapped to UUIDv7
- **Token Opacity**: Tokens are cryptographically bound to the PII they represent

### 3. Immutable Entry Design
- **Immutable PII Entries**: All PII inserts are immutable - no updates or modifications allowed
- **Token Binding**: Once a PII entry is encrypted, it is cryptographically bound to a unique token_id
- **One-to-One Mapping**: Each encrypted PII entry has exactly one immutable token_id
- **Audit Trail Integrity**: Immutability ensures audit trail cannot be tampered with or modified
- **Compliance Benefit**: Immutability satisfies regulatory requirements for data integrity and non-repudiation
- **No Delete Option**: PII entries cannot be deleted from vault; upstream services manage retention
- **Cryptographic Guarantee**: Token_id ensures the same PII always produces the same token (deterministic encryption)

### 4. UUID v7 Indexing
- **UUID v7 Support**: All operations use UUID v7 for correlation and tracing
- **Request/Response Mapping**: Each PII entry in batch operations mapped to UUID v7
- **Distributed Tracing**: UUID v7 enables end-to-end request tracking
- **Time-Ordered**: UUID v7 provides sortable, time-based identifiers

### 4. Flexible PII Data Management via JSON Blobs

While the following PII data types are supported, all data is managed as **JSON entries stored in a single encrypted blob** rather than separate database columns per type. This design choice provides significant operational and cost benefits:

**Supported PII Data Types:**
- Email addresses
- Phone numbers
- Social Security Numbers (SSN)
- Credit card numbers (PCI DSS compliant)
- Passport numbers
- Driver's license numbers
- Date of birth
- Home addresses
- Bank account numbers
- Custom PII types (application-specific)

**JSON Blob Structure:**
```json
{
  "email": "user@example.com",
  "phone": "+1-555-123-4567",
  "ssn": "123-45-6789",
  "credit_card": "4111-1111-1111-1111",
  "dob": "1990-01-15",
  "address": "123 Main St, City, State 12345"
}
```

**Benefits of JSON Blob Approach:**

1. **KMS Cost Reduction**
   - Single KMS decrypt call for all related PII types
   - Traditional approach: 1 decrypt per column × 6 types = 6 KMS calls
   - JSON approach: 1 decrypt for entire blob = 1 KMS call
   - Cost reduction: ~83% fewer KMS API calls

2. **Downstream Performance Optimization**
   - No joins required to retrieve related PII data
   - Single decryption yields all connected data
   - Data lake queries process denormalized PII in one fetch
   - Eliminates N+1 query problems

3. **Flexibility**
   - Support multiple PII types per entity without schema changes
   - Add new PII types without DDL migrations
   - Customer-specific PII types as additional JSON properties
   - No database column management overhead

4. **Immutability Enforcement**
   - Single encrypted blob per token_id
   - Entire PII set is immutable together
   - Type information included in encrypted data
   - No separate type metadata to manage

5. **Query Efficiency**
   - Type information stored within encrypted data
   - No secondary index lookups for type filtering
   - Batch decrypt returns all requested data at once
   - Network efficiency: single response payload

**Type Discovery:**
When decrypting, the JSON structure reveals all PII types included in that token. No database schema required to understand what types exist in the vault.

### 5. Audit & Logging
- **Complete Audit Trail**: Every encryption/decryption operation logged
- **Operation Tracking**: Track requestor, timestamp, PII type, and operation result
- **Compliance Reporting**: Generate audit reports for regulatory purposes
- **Security Monitoring**: Alert on unusual access patterns

### 6. Soft-Delete with Admin Recovery
- **Soft-Delete Tokens**: Mark tokens as deleted without destroying encrypted data
- **Admin Recovery**: Dedicated admin endpoints to view soft-deleted tokens
- **Deletion Timestamp**: Track when tokens were marked for deletion
- **Audit Trail**: Complete history of deletion events
- **Compliance**: Supports right-to-be-forgotten while maintaining audit integrity
- **Data Preservation**: Encrypted PII remains in vault for recovery or legal holds

### 7. Event Streaming for Data Replication
- **Token Lifecycle Events**: Stream events for token creation, deletion, expiration
- **Event Types**:
  - `TOKEN_CREATED`: PII encrypted and token issued
  - `TOKEN_DELETED`: Token marked as soft-deleted
  - `TOKEN_EXPIRED`: Token expiration triggered
  - `TOKEN_INVALIDATED`: Token invalidated via key rotation
- **CDC Integration**: Change Data Capture streams encrypted PII events from vault
- **PCI-Safe Decryption Zone**: Data lake maintains separate PCI-safe zone for decryption
- **Batch Decryption**: Decrypt events in PCI-safe zone using batch decrypt endpoint
- **Data Lake Replication**: Encrypted PII replicated to data lake via CDC + decryption
- **Event Schema**: Standardized event format with request_id, timestamp, token_id, event_type
- **Delivery Guarantee**: At-least-once delivery semantics via event log

### 8. Data Retention & Operational Tier Partitioning
- **Soft-Delete Retention Period**: Soft-deleted tokens retained for compliance (typically 1 year)
- **Operational Tier Partitioning**: Database partitioned by soft-delete date for efficient management
- **Hot Tier (0-30 days)**: Soft-deleted data in hot tier, quickly accessible for recovery
- **Warm Tier (30 days - N months)**: Soft-deleted data in warm tier, occasional access
- **Cold Tier (N months+)**: Soft-deleted data in cold tier, archive storage
- **Hard Deletion**: After retention period (N months, typically 1 year), hard delete soft-deleted records
- **Compliance Window**: Retention period satisfies GDPR, CCPA, HIPAA, PCI DSS requirements
- **Storage Optimization**: Tier partitioning reduces hot storage costs while maintaining compliance
- **Audit Trail**: Hard deletion events logged for compliance validation

### 9. Compliance Features
- **PCI DSS v4 Compliance**: Full compliance with PCI DSS version 4 requirements
- **GDPR Support**: Encryption enables GDPR compliance for data protection
- **CCPA/CPRA Support**: Secure encryption for California privacy requirements
- **HIPAA Support**: Encryption standards suitable for healthcare data
- **Audit Trail**: Comprehensive logging for compliance validation

## Technical Specifications

### Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                  Upstream Client Services                         │
│  (Own all data storage, search, indexing, retention policy)      │
├──────────────────────────────────────────────────────────────────┤
│                    API Gateway / Load Balancer                    │
├──────────────────────────────────────────────────────────────────┤
│              Spring Boot 4.0 REST API Layer                       │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Controllers (REST Endpoints)                              │  │
│  │ - /encrypt          (single PII → token)                  │  │
│  │ - /decrypt          (token → single PII)                  │  │
│  │ - /batch/encrypt    (multiple PII → tokens + UUIDv7)      │  │
│  │ - /batch/decrypt    (tokens + UUIDv7 → PII)              │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Service Layer (Business Logic)                            │  │
│  │ - EncryptionService (AES-256 encryption)                  │  │
│  │ - KmsService (AWS KMS integration)                        │  │
│  │ - AuditService (logging and compliance)                   │  │
│  │ - TokenService (token generation/validation)              │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Repository Layer (Data Access)                            │  │
│  │ - AuditLogRepository                                      │  │
│  │ - TokenMetadataRepository (token tracking)                │  │
│  └────────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│                  Database Layer                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ PostgreSQL (Primary Data Storage)                           │ │
│  │                                                             │ │
│  │ 1. KMS Key Management Table (pii_vault_kms_keys)           │ │
│  │    - Stores KMS key metadata (alias, arn, is_active, etc.) │ │
│  │    - Tracks key rotation history                           │ │
│  │    - Active key used for all encryption operations         │ │
│  │                                                             │ │
│  │ 2. PII Data Table (pii_data)                               │ │
│  │    - pii_token_id (primary key, immutable)                 │ │
│  │    - encrypted_data (JSON blob, encrypted)                 │ │
│  │    - created_date (immutable timestamp)                    │ │
│  │    - is_deleted (soft-delete flag)                         │ │
│  │    - deleted_date (timestamp when soft-deleted)            │ │
│  │    - owner (tenant/requestor identifier)                   │ │
│  │                                                             │ │
│  │ 3. Audit Logs Table (audit_logs)                           │ │
│  │    - Operation tracking for compliance                     │ │
│  │    - Immutable audit trail                                 │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ AWS KMS (External Key Management Service)                   │ │
│  │ - Master key encryption and rotation                        │ │
│  │ - CloudTrail logging of all KMS operations                 │ │
│  │ - Automatic annual key rotation                            │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### Double Encryption Flow

```
Encryption Flow:
1. Client sends PII data to /encrypt endpoint
2. Application encrypts with AES-256-GCM using application key
3. Application key is itself encrypted with AWS KMS master key
4. Encrypted data stored with metadata
5. Opaque token returned to client
6. Client stores token; PII Vault owns encrypted data

Decryption Flow:
1. Client sends opaque token to /decrypt endpoint
2. Token is validated and linked to encrypted data
3. AWS KMS decrypts the application key (Layer 1)
4. Application key decrypts the PII data (Layer 2)
5. Decrypted PII returned to client
6. Operation logged for audit trail
```

### Database Layer Specification

PII Vault stores three critical types of data in PostgreSQL:

#### 1. KMS Key Management Table (`pii_vault_kms_keys`)

Tracks all KMS key metadata required for encryption and rotation management.

**Purpose:** Maintains a registry of all active and historical encryption keys with metadata needed for:
- Determining which key to use for encryption (active key)
- Decryption of data encrypted with historical keys
- Key rotation tracking and history
- Admin visibility into key lifecycle

**Schema:**
```sql
CREATE TABLE pii_vault_kms_keys (
  id BIGSERIAL PRIMARY KEY,
  key_alias VARCHAR(255) NOT NULL UNIQUE,        -- ALIAS_YYYYMMDD (e.g., VAULT_20260707)
  key_arn VARCHAR(500) NOT NULL UNIQUE,          -- AWS KMS key ARN
  key_id VARCHAR(255) NOT NULL UNIQUE,           -- AWS KMS key ID
  is_active BOOLEAN NOT NULL DEFAULT false,      -- Current active key for encryption
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),   -- Key creation time
  activated_at TIMESTAMP,                        -- When key became active (activation date)
  deactivated_at TIMESTAMP,                      -- When key stopped being active (deactivation date)
  reason VARCHAR(500),                           -- Reason for activation/deactivation
  rotations_count INTEGER NOT NULL DEFAULT 0,    -- Total rotations performed
  last_rotation_at TIMESTAMP,                    -- Last time this key was rotated
  metadata JSONB                                 -- Additional key metadata
);
```

**Key Characteristics:**
- Alias follows pattern: `VAULT_{YYYYMMDD}` where YYYYMMDD is creation date
- Only ONE key should be `is_active = true` at any given time
- All keys retained indefinitely for decryption of historical data
- `activated_at` timestamp shows when rotation made this key active
- `deactivated_at` timestamp shows when next rotation retired this key
- Audit trail preserved for compliance

#### 2. PII Data Storage Table (`pii_data`)

Stores all encrypted PII data with immutability enforced.

**Purpose:** Immutably stores encrypted PII entries with lifecycle tracking for:
- Retrieving encrypted data for decryption operations
- Tracking token_id mapping to encrypted data
- Soft-delete capability for user privacy
- Operational tier partitioning (hot/warm/cold storage)
- Compliance with data retention requirements

**Schema:**
```sql
CREATE TABLE pii_data (
  pii_token_id UUID PRIMARY KEY,                 -- Immutable unique token identifier
  encrypted_data TEXT NOT NULL,                  -- Encrypted JSON blob (encrypted with active key)
  created_date TIMESTAMP NOT NULL DEFAULT NOW(), -- Immutable creation timestamp
  is_deleted BOOLEAN NOT NULL DEFAULT false,     -- Soft-delete flag
  deleted_date TIMESTAMP,                        -- When token was soft-deleted (NULL if not deleted)
  owner VARCHAR(255),                            -- Tenant/requestor identifier
  created_by VARCHAR(255),                       -- User who requested encryption
  kms_key_id VARCHAR(255) NOT NULL,              -- Which KMS key was used for encryption
  CONSTRAINT pii_data_immutable CHECK (
    -- Enforce immutability: once created_date is set, it never changes
    TRUE
  )
);
```

**Key Characteristics:**
- `pii_token_id`: UUID primary key, immutable, generated deterministically from encrypted data
- `encrypted_data`: Entire PII JSON blob encrypted as single unit
- `created_date`: Immutable timestamp, never updates after creation
- `is_deleted`: Boolean flag for soft-delete (no permanent deletion)
- `deleted_date`: Timestamp of soft-delete operation
- `owner`: Identifies tenant or requestor for multi-tenant scenarios
- `kms_key_id`: References which key encrypted this data (needed for decryption)
- Operational tier partitioning by `deleted_date` for hot/warm/cold storage

**Soft-Delete Behavior:**
- When user requests deletion, `is_deleted` set to `true` and `deleted_date` recorded
- Encrypted PII data remains in database for recovery/audit
- Admin endpoints can query soft-deleted records
- Hard deletion happens after N months (typically 1 year) via tier partitioning

#### 3. Audit Logs Table (`audit_logs`)

Maintains immutable audit trail of all operations for compliance.

**Purpose:** Records all encryption/decryption operations for:
- PCI DSS v4 audit trail compliance (Requirement 10)
- GDPR, HIPAA, CCPA audit requirements
- Operational debugging and troubleshooting
- Security incident investigation
- Forensic analysis of token usage

**Schema:**
```sql
CREATE TABLE audit_logs (
  event_id BIGSERIAL PRIMARY KEY,
  timestamp TIMESTAMP NOT NULL DEFAULT NOW(),    -- When operation occurred
  operation_type VARCHAR(50) NOT NULL,           -- ENCRYPT, DECRYPT, SOFT_DELETE, etc.
  pii_token_id UUID,                             -- Token involved in operation
  owner VARCHAR(255),                            -- Who performed operation
  request_id UUID NOT NULL,                      -- Request correlation ID
  status VARCHAR(50),                            -- SUCCESS, FAILURE
  error_message TEXT,                            -- Error details if failed
  ip_address VARCHAR(45),                        -- Client IP for audit
  user_agent TEXT,                               -- Client user agent
  metadata JSONB                                 -- Additional operation context
);
```

**Key Characteristics:**
- Immutable audit log (no updates/deletes)
- Every encryption/decryption operation logged
- Request correlation via `request_id`
- Success/failure tracking for security monitoring
- Complete context preserved for investigations
- Meets all regulatory audit requirements

### Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 4.0.0 |
| Spring Framework | Spring Framework | 7.0.0 |
| Java | OpenJDK | 21+ |
| Build Tool | Maven | 3.6+ |
| ORM | Hibernate/JPA | Latest |
| Database | PostgreSQL | 14+ |
| Key Management | AWS KMS | Latest |
| Encryption | Bouncy Castle (AES-256-GCM) | Latest |
| UUID | UUID v7 | Latest |
| Testing | JUnit 5, Mockito | Latest |

**Database Design:** See [DESIGN.md](./DESIGN.md) for detailed schema definitions, table layouts, and immutability enforcement patterns.

## API Endpoints

### Single Operations

#### 1. Encrypt PII Data
```
POST /api/v1/encrypt
Content-Type: application/json

Request:
{
  "piiType": "email",
  "piiValue": "user@example.com",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0...",
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "piiType": "email",
  "encryptedAt": "2026-07-07T08:00:00Z",
  "expiresAt": null,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 2. Decrypt Token to PII
```
POST /api/v1/decrypt
Content-Type: application/json

Request:
{
  "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0...",
  "requestId": "550e8400-e29b-41d4-a716-446655440002"
}

Response (200 OK):
{
  "piiType": "email",
  "piiValue": "user@example.com",
  "decryptedAt": "2026-07-07T08:05:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### Batch Operations

#### 3. Batch Encrypt PII Data
```
POST /api/v1/batch/encrypt
Content-Type: application/json

Request:
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
    }
  ]
}

Response (200 OK):
{
  "requestId": "550e8400-e29b-41d4-a716-446655440003",
  "encryptedAt": "2026-07-07T08:10:00Z",
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440004",
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440006",
      "piiType": "email",
      "success": true
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440005",
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6InBob25lIn0...",
      "tokenId": "550e8400-e29b-41d4-a716-446655440007",
      "piiType": "phone",
      "success": true
    }
  ]
}
```

#### 4. Batch Decrypt Tokens
```
POST /api/v1/batch/decrypt
Content-Type: application/json

Request:
{
  "requestId": "550e8400-e29b-41d4-a716-446655440008",
  "entries": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440009",
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6ImVtYWlsIn0..."
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440010",
      "token": "eyJhbGciOiJBMjU2R0NNIiwicGlpVHlwZSI6InBob25lIn0..."
    }
  ]
}

Response (200 OK):
{
  "requestId": "550e8400-e29b-41d4-a716-446655440008",
  "decryptedAt": "2026-07-07T08:15:00Z",
  "results": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440009",
      "piiType": "email",
      "piiValue": "user1@example.com",
      "success": true
    },
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440010",
      "piiType": "phone",
      "piiValue": "+1-555-123-4567",
      "success": true
    }
  ]
}
```

### Admin Operations

#### 5. Soft-Delete Token (Admin Only)
```
DELETE /api/v1/admin/tokens/{tokenId}
Authorization: Bearer {admin-token}
Content-Type: application/json

Request:
{
  "reason": "User requested deletion",
  "requestId": "550e8400-e29b-41d4-a716-446655440011"
}

Response (200 OK):
{
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "deletedAt": "2026-07-07T08:20:00Z",
  "reason": "User requested deletion",
  "recoverable": true,
  "requestId": "550e8400-e29b-41d4-a716-446655440011"
}
```

**Soft-Delete Behavior:**
- Token marked as deleted but encrypted data preserved
- Admin-only operation (requires elevated permissions)
- Returns immutable deletion timestamp and reason
- Encrypted PII remains in vault for recovery or legal holds
- Cannot be undone (immutable deletion record)
- Supports GDPR right-to-be-forgotten compliance

#### 6. Get Token Lifecycle Events (Admin Only)
```
GET /api/v1/admin/tokens/{tokenId}/events
Authorization: Bearer {admin-token}

Query Parameters:
- from: ISO8601 timestamp (optional)
- to: ISO8601 timestamp (optional)
- eventTypes: comma-separated list (optional, e.g., TOKEN_CREATED,TOKEN_DELETED)

Response (200 OK):
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
  "totalEvents": 3
}
```

#### 7. Stream Token Events (Admin Only)
```
GET /api/v1/admin/events/stream
Authorization: Bearer {admin-token}

Query Parameters:
- since: ISO8601 timestamp (required, start from this time)
- eventTypes: comma-separated list (optional)
- tokenIds: comma-separated list (optional)
- limit: max events per poll (default: 1000, max: 10000)

Response (200 OK, Server-Sent Events):
{
  "events": [
    {
      "eventId": 12348,
      "tokenId": "550e8400-e29b-41d4-a716-446655440012",
      "eventType": "TOKEN_CREATED",
      "eventTimestamp": "2026-07-07T08:25:00Z",
      "requestId": "550e8400-e29b-41d4-a716-446655440013",
      "payload": {
        "piiType": "email",
        "tokenId": "550e8400-e29b-41d4-a716-446655440012"
      }
    },
    {
      "eventId": 12349,
      "tokenId": "550e8400-e29b-41d4-a716-446655440014",
      "eventType": "TOKEN_CREATED",
      "eventTimestamp": "2026-07-07T08:26:00Z",
      "requestId": "550e8400-e29b-41d4-a716-446655440015",
      "payload": {
        "piiType": "phone",
        "tokenId": "550e8400-e29b-41d4-a716-446655440014"
      }
    }
  ],
  "nextEventId": 12350,
  "hasMore": false
}
```

**Event Streaming Features:**
- Data lake systems consume events via polling or webhook
- At-least-once delivery guarantee via immutable event log
- Events contain tokenId and metadata needed for replication
- Encrypted PII retrieved via batch decrypt endpoint
- Enables replicated encrypted copy in data lake
- Immutable event log preserves complete history

## Immutability Model

### Design Principles

**Immutability as Core Requirement:**
- All PII entries inserted into PII Vault are immutable by design
- Once encrypted and assigned a token_id, the entry cannot be modified
- Immutability is enforced at multiple levels: application, database, and cryptographic

### Immutability Benefits

1. **Security**
   - Prevents tampering with encrypted PII data
   - Eliminates update-based attack vectors
   - Ensures encrypted data integrity throughout lifecycle

2. **Compliance**
   - Satisfies non-repudiation requirements
   - Enables audit trail integrity verification
   - Meets regulatory requirements for data integrity (PCI DSS, GDPR, HIPAA)
   - Provides cryptographic proof of data authenticity

3. **Operational**
   - Simplifies data management - no complex update logic
   - Reduces debugging complexity - no state changes to track
   - Enables better caching strategies
   - Improves performance predictability

### Immutability Enforcement

**Application Level:**
- No update endpoints (PUT/PATCH for encryption data)
- Only encrypt and decrypt operations allowed
- Validation on all requests to prevent modification attempts

**Database Level:**
- CHECK constraints enforce immutability flags
- Unique constraints on token_id ensure one-to-one mapping
- Immutable columns prevent accidental updates via direct SQL

**Cryptographic Level:**
- token_id is derived from encrypted data hash
- Same PII with same encryption key produces same token_id
- Deterministic encryption ensures consistency

### What Happens to Data Lifecycle?

| Operation | PII Vault | Upstream Service |
|-----------|-----------|------------------|
| **Create/Encrypt** | ✅ Stores immutably with token_id | Receives token, manages storage |
| **Retrieve/Decrypt** | ✅ Returns decrypted PII | Manages access control |
| **Update** | ❌ NOT ALLOWED | Must re-encrypt with new token_id |
| **Delete** | ❌ NOT ALLOWED | Must manage retention/deletion |
| **Search** | ❌ NOT SUPPORTED | Must use upstream indexes |
| **Audit** | ✅ Logs all operations | May log in their systems too |

### Update Strategy for Immutable Data

If an upstream service needs to update PII:

```
1. Upstream service retrieves current PII via token
2. Decrypts PII (if needed) using old token
3. Modifies PII in their system
4. Calls encrypt endpoint with NEW PII
5. Receives NEW token_id
6. Updates their system to use new token_id
7. Old token can be marked for deletion (upstream's responsibility)
```

### Token Lifecycle Management

**Token Identity:**
- `token_id`: Unique identifier for encrypted PII entry
- Immutably created when PII is encrypted
- Never changes for the lifetime of the encrypted entry
- Can only be invalidated through key rotation (Phase 2)

**Token States:**
- `ACTIVE`: Token is valid and can be used for decryption
- `EXPIRED`: Token has expired (if expiration enabled)
- `INVALID`: Token has been explicitly invalidated
- Creation and initial state are immutable

---

## Security & Compliance

### PCI DSS v4 Compliance
- **Requirement 3**: Strong cryptography - AES-256-GCM encryption
- **Requirement 4**: Encryption in transit - HTTPS/TLS 1.3+
- **Requirement 8**: User identification and access control
- **Requirement 10**: Logging and monitoring - audit trail of all operations
- **Requirement 12**: Security policies - comprehensive documentation

### Double Encryption Benefits
1. **Defense in Depth**: Two independent encryption layers
2. **Key Isolation**: Application key and KMS master key are separate
3. **Compliance**: Meets stringent regulatory requirements
4. **Rotation**: Automatic key rotation without decryption/re-encryption
5. **Threat Resilience**: Compromise of one key layer doesn't expose PII

### Key Management (AWS KMS)
- **Master Key**: Stored and managed in AWS KMS
- **Key Rotation**: Automatic annual rotation
- **Audit Trail**: CloudTrail logging of all KMS operations
- **Regional**: Keys can be replicated across regions
- **Access Control**: IAM policies control key access

## Non-Functional Requirements

### Performance
- **Encryption Time**: < 50ms per operation (p95)
- **Decryption Time**: < 50ms per operation (p95)
- **Batch Throughput**: 10,000+ operations/second
- **Latency**: < 100ms end-to-end response time

### Reliability
- **Availability**: 99.95% uptime SLA
- **Recovery Time Objective (RTO)**: < 5 minutes
- **Recovery Point Objective (RPO)**: < 1 minute
- **Data Durability**: 99.9999999999% (12 nines)

### Security
- **Encryption Standard**: AES-256-GCM
- **Key Rotation**: Annual via AWS KMS
- **TLS**: 1.3+ for all communications
- **Audit Logging**: 100% of operations logged
- **Token Expiration**: Configurable per deployment

### Scalability
- **Horizontal Scaling**: Stateless design
- **Database**: PostgreSQL with connection pooling, read replicas
- **Data Partitioning**: Operational tier partitioning (hot/warm/cold) for soft-deleted data
- **Load Balancing**: Support for multiple instances
- **Backup & Recovery**: Regular backups with point-in-time recovery

## Phase 1 (MVP) - Q3 2026

### Core Features
- [x] AES-256-GCM encryption at application layer
- [x] Single encrypt operation
- [x] Single decrypt operation
- [x] Batch encrypt operation with UUID v7 mapping
- [x] Batch decrypt operation with UUID v7 mapping
- [x] Basic audit logging
- [x] REST API endpoints
- [x] Spring Security integration

### Deliverables
- Executable Spring Boot JAR
- API documentation
- Basic unit tests
- Development setup guide

## Phase 2 - Q4 2026

### Enhanced Features
- [ ] AWS KMS integration for key management
- [ ] Double encryption (application + KMS)
- [ ] Token expiration and lifecycle management
- [ ] Comprehensive audit reporting
- [ ] JWT/OAuth2 authentication
- [ ] Rate limiting and throttling
- [ ] Token cache optimization
- [ ] Monitoring and alerting

### Deliverables
- Docker images with KMS support
- KMS configuration guide
- Integration tests
- Monitoring dashboard

## Phase 3 - Q1 2027

### Compliance & Hardening
- [ ] PCI DSS v4 certification
- [ ] GDPR compliance validation
- [ ] CCPA/CPRA compliance validation
- [ ] HIPAA compliance validation
- [ ] Penetration testing
- [ ] Security audit
- [ ] Performance optimization

### Deliverables
- Compliance audit reports
- Security audit results
- Operational runbooks
- Performance benchmarks

## Success Metrics

| Metric | Target | Timeline |
|--------|--------|----------|
| Encryption Latency (p95) | < 50ms | Phase 1 |
| Decryption Latency (p95) | < 50ms | Phase 1 |
| Batch Throughput | 10,000+ ops/sec | Phase 1 |
| Audit Log Completeness | 100% | Phase 1 |
| System Availability | 99.95% | Phase 2 |
| PCI DSS Compliance | Complete | Phase 3 |
| Production Deployments | 10+ | Phase 3 |

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| KMS key compromise | Low | Critical | AWS KMS security, CloudTrail monitoring |
| Token token leak | Low | High | Token expiration, audit alerts |
| Performance degradation | Medium | High | Caching, optimization, monitoring |
| Regulatory non-compliance | Low | Critical | Regular compliance audits |
| Service unavailability | Low | Critical | Multi-region setup, automated failover |

## Constraints & Assumptions

### Assumptions
- Organizations have AWS account with KMS access
- Java 21+ runtime available
- PostgreSQL or compatible database
- HTTPS/TLS termination at load balancer
- Redis available for caching (optional)

### Constraints
- Initial AWS KMS region limited (multi-region in Phase 3)
- No support for HSM (future enhancement)
- Token size limited to 4KB (HTTP header constraints)
- Batch operations limited to 1,000 entries per request

## Integration Requirements

### For Upstream Services
1. **Token Management**: Securely store returned tokens
2. **Key Rotation**: Handle token invalidation during key rotation
3. **Audit Logging**: Implement own audit trail for data access
4. **Search/Indexing**: Implement search using non-encrypted fields
5. **Data Retention**: Implement own retention policies
6. **Error Handling**: Implement retry logic with exponential backoff

### No Blind Indexes or Searchable Encryption
- PII Vault does not support encrypted search
- Upstream services must implement search on non-encrypted data
- Blind indexes are NOT supported by design
- All search capabilities remain with upstream services

## Glossary

- **PII**: Personally Identifiable Information
- **Token**: Opaque encrypted representation of PII data
- **AES-256-GCM**: Advanced Encryption Standard with 256-bit key and Galois/Counter Mode
- **KMS**: Key Management Service (AWS)
- **UUID v7**: Time-based sortable UUID
- **PCI DSS**: Payment Card Industry Data Security Standard
- **Double Encryption**: Two independent encryption layers
- **Audit Trail**: Complete log of all operations

## Appendix

### Dependencies
- Spring Boot 4.0.0
- Spring Framework 7.0.0
- Jakarta EE 10
- AWS SDK for KMS
- Bouncy Castle for cryptography
- PostgreSQL JDBC Driver
- JUnit 5
- Mockito

### References
- [PCI DSS v4.0 Standard](https://www.pcisecuritystandards.org/)
- [AWS KMS Documentation](https://docs.aws.amazon.com/kms/)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [RFC 4648 - Base64 Data Encodings](https://tools.ietf.org/html/rfc4648)

---

**Last Updated**: 2026-07-07
**Version**: 1.0.0

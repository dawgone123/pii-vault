# PII Vault - KMS Encryption Strategy

This document outlines the KMS key management and rotation strategy for PII Vault, including operational procedures, rotation schedules, and pitfall mitigation.

---

## Overview

PII Vault implements a **multi-layered encryption strategy** using AWS KMS as the key management service. This document covers:
- KMS key generation and lifecycle
- Automatic key rotation by AWS KMS
- Custom key rotation scheduling via cron jobs
- Pitfall mitigation and operational safeguards

---

## Double Encryption Architecture

### Encryption Layers

**Layer 1: Application-Level Encryption (AES-256-GCM)**
- Encrypts PII data at the application level
- Uses an application-managed encryption key
- Fast encryption/decryption (no AWS API calls)

**Layer 2: KMS-Managed Key Encryption (AWS KMS)**
- Encrypts the application key using AWS KMS
- Master key never leaves KMS
- Provides key rotation and audit logging
- Ensures separation of concerns

### Encryption Flow

```
PII Data
  ↓
[AES-256-GCM Encrypt with Application Key] → Encrypted Data
  ↓
Application Key
  ↓
[KMS Encrypt with Master Key] → Encrypted Application Key
  ↓
Store: Encrypted Data + Encrypted Application Key + KeyId Reference
```

### Decryption Flow

```
Encrypted Data + Encrypted Application Key + KeyId
  ↓
[KMS Decrypt with KeyId] → Application Key
  ↓
[AES-256-GCM Decrypt with Application Key] → Original PII Data
```

---

## KMS Key Generation Strategy

### Key Creation Schedule

**Primary Key Rotation Interval:** Every **N days** (N = 7-30 days, configurable)
- **Conservative (7 days)**: Higher security, more keys to manage
- **Standard (14 days)**: Balanced approach, recommended
- **Aggressive (30 days)**: Lower management overhead

**Why Generate New Keys?**
1. **Security by Rotation**: Reduces blast radius if a key is compromised
2. **Compliance**: Meets regulatory requirements for key rotation
3. **Defense in Depth**: Multiple keys in use reduces single-point failure
4. **Audit Trail**: Each key tied to specific time period

### Key Naming Convention

```
pii-vault-encryption-key-{YYYY-MM-DD}
pii-vault-encryption-key-2026-07-08
pii-vault-encryption-key-2026-07-15
pii-vault-encryption-key-2026-07-22
```

**Format Benefits:**
- Human-readable creation date
- Easy to identify key age
- Chronological sorting works naturally
- Clear correlation with encryption period

### Automatic KMS Key Rotation

**KMS Native Rotation (M days, M typically 90 days)**

AWS KMS automatically rotates Customer Managed Keys:
- Rotation happens transparently every **M days** (default: 90 days, configurable)
- Old key material remains available for decryption
- All encryption operations use the latest key version
- No application code changes required
- CloudTrail logs all rotation events

**How KMS Rotation Works:**
```
Day 1: Key material v1 created
  ↓
Day 90: New key material v2 created (automatic)
  - v1 remains usable for decryption
  - v2 used for all new encryptions
  ↓
Day 180: New key material v3 created (automatic)
  - v1, v2 remain usable for decryption
  - v3 used for all new encryptions
```

### Relationship Between N-day and M-day Rotations

| Strategy | Interval | Purpose | AWS KMS Involvement |
|----------|----------|---------|-------------------|
| **Custom N-day Rotation** | 7-30 days | Create NEW customer-managed keys | New KeyId every N days |
| **AWS M-day Rotation** | 90 days | Rotate existing key material | Automatic key version cycling |
| **Combined Strategy** | Both | Maximum security + compliance | Multiple keys + automatic versioning |

**Timeline Example (N=14 days, M=90 days):**
```
Day 1:  Create KeyId-1
Day 14: Create KeyId-2 (application switches to new key)
Day 28: Create KeyId-3
Day 42: Create KeyId-4
Day 56: Create KeyId-5
Day 70: Create KeyId-6
Day 84: Create KeyId-7
Day 90: KeyId-1 automatic internal rotation (v1 → v2, but same KeyId)
Day 98: Create KeyId-8
...
```

---

## Cron-Based Key Rotation Implementation

### Scheduled Key Creation

**Cron Expression (Every 14 days at 2 AM UTC):**
```bash
0 2 */14 * * /opt/pii-vault/bin/rotate-kms-key.sh
```

**Monthly Alternative (1st of each month):**
```bash
0 2 1 * * /opt/pii-vault/bin/rotate-kms-key.sh
```

### Rotation Script Workflow

```bash
#!/bin/bash
# rotate-kms-key.sh

set -e

# 1. Generate new key in AWS KMS
NEW_KEY_ID=$(aws kms create-key \
  --description "PII Vault encryption key $(date +%Y-%m-%d)" \
  --key-policy file://key-policy.json \
  --query 'KeyMetadata.KeyId' \
  --output text)

# 2. Create alias for easy reference
NEW_KEY_ALIAS="alias/pii-vault-encryption-key-$(date +%Y-%m-%d)"
aws kms create-alias \
  --alias-name "$NEW_KEY_ALIAS" \
  --target-key-id "$NEW_KEY_ID"

# 3. Store new KeyId in application configuration
aws ssm put-parameter \
  --name "/pii-vault/kms/current-key-id" \
  --value "$NEW_KEY_ID" \
  --type "String" \
  --overwrite

# 4. Log rotation event
echo "$(date): KMS key rotation completed. New KeyId: $NEW_KEY_ID" >> /var/log/pii-vault/kms-rotation.log

# 5. Notify monitoring/alerting system
curl -X POST https://monitoring.example.com/events \
  -H "Content-Type: application/json" \
  -d "{
    \"event\": \"kms_key_rotated\",
    \"new_key_id\": \"$NEW_KEY_ID\",
    \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
  }"
```

### Key Policy Management

**Critical: Copy IAM Key Policy to New Key**

When creating a new key, you MUST copy the IAM policy from the previous key:

```bash
# 1. Get policy from old key
OLD_KEY_ID="arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012"
aws kms get-key-policy \
  --key-id "$OLD_KEY_ID" \
  --policy-name default \
  > old-key-policy.json

# 2. Update Principal ARNs to new key (if needed)
# 3. Apply to new key
aws kms put-key-policy \
  --key-id "$NEW_KEY_ID" \
  --policy-name default \
  --policy file://old-key-policy.json
```

**Typical Key Policy (Example):**
```json
{
  "Sid": "Allow PII Vault Application to use the key",
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::123456789012:role/pii-vault-app-role"
  },
  "Action": [
    "kms:Decrypt",
    "kms:GenerateDataKey",
    "kms:DescribeKey"
  ],
  "Resource": "*"
}
```

---

## Pitfall Mitigation

### Pitfall 1: AWS KMS Resource Quotas

**Problem:**
AWS accounts have a default limit of 10,000 Customer Managed Keys per region. Creating a new key every N days could theoretically exhaust this limit.

**Risk Analysis:**
- Creating 1 key every 14 days = 26 keys/year
- Creating 1 key every 7 days = 52 keys/year
- Even at 52 keys/year, it takes 192 years to hit 10,000 key limit
- **Practical Risk: VERY LOW**

**Mitigation Strategies:**

1. **Set a Retention Policy on Old Keys**
   ```bash
   # Archive keys older than 2 years
   for key in $(aws kms list-keys --query 'Keys[].KeyId' --output text); do
     creation_date=$(aws kms describe-key --key-id "$key" --query 'KeyMetadata.CreationDate' --output text)
     if [[ $(date -d "$creation_date" +%s) -lt $(date -d "2 years ago" +%s) ]]; then
       # Schedule key for deletion (30-day grace period)
       aws kms schedule-key-deletion --key-id "$key" --pending-window-in-days 30
     fi
   done
   ```

2. **Monitor Key Count**
   ```bash
   # Alert if key count exceeds threshold
   KEY_COUNT=$(aws kms list-keys --query 'length(Keys)' --output text)
   if [[ $KEY_COUNT -gt 500 ]]; then
     send_alert "KMS key count approaching limits: $KEY_COUNT/10000"
   fi
   ```

3. **Request Quota Increase**
   - Contact AWS Support to increase the 10,000 key limit per region
   - No charge for quota increases
   - Typical response: 24-48 hours

**Recommended Action:**
- No immediate action needed
- Monitor key count annually
- Request quota increase at 20% utilization (2,000+ keys)

---

### Pitfall 2: KMS Key Policies and IAM Permissions

**Problem:**
Every new KMS key has its own individual IAM Key Policy. If the new key doesn't have the correct permissions, the PII Vault application will suddenly lose the ability to use the new key.

**Risk Scenario:**
```
Day 1: Application uses KeyId-1 (works fine, has proper permissions)
Day 14: New KeyId-2 created WITHOUT proper IAM policy
Day 15: Application tries to use KeyId-2 → KMS ACCESS DENIED
Result: PII encryption/decryption FAILS for all new operations
```

**Mitigation Strategies:**

1. **Automated Policy Copy (Recommended)**
   ```bash
   #!/bin/bash
   # Automatically copy policy from previous key to new key
   
   PREVIOUS_KEY_ID=$(aws ssm get-parameter \
     --name "/pii-vault/kms/previous-key-id" \
     --query 'Parameter.Value' \
     --output text)
   
   # Get policy from previous key
   aws kms get-key-policy \
     --key-id "$PREVIOUS_KEY_ID" \
     --policy-name default \
     --output text > key-policy.json
   
   # Create new key with policy
   NEW_KEY_ID=$(aws kms create-key \
     --description "PII Vault key $(date +%Y-%m-%d)" \
     --key-policy file://key-policy.json \
     --query 'KeyMetadata.KeyId' \
     --output text)
   ```

2. **Pre-Create Policy Template**
   - Store baseline key policy in version control
   - Use CloudFormation or Terraform for infrastructure-as-code
   - Ensures consistency across all keys

3. **Permission Validation Test**
   ```bash
   # After key creation, test permissions
   TEST_DATA="test-encryption-data"
   
   if aws kms encrypt \
     --key-id "$NEW_KEY_ID" \
     --plaintext "$TEST_DATA" \
     --query 'CiphertextBlob' \
     > /dev/null 2>&1; then
     echo "✓ New key has proper permissions"
   else
     echo "✗ ERROR: New key lacks required permissions!"
     send_critical_alert "KMS key rotation failed: missing permissions on $NEW_KEY_ID"
     exit 1
   fi
   ```

4. **Gradual Migration**
   ```
   Phase 1: Create new key (but don't use yet)
   Phase 2: Validate permissions (test encrypt/decrypt)
   Phase 3: Update application configuration
   Phase 4: Monitor for 24 hours (watch logs/metrics)
   Phase 5: Mark old key as deprecated
   ```

5. **Monitoring and Alerting**
   ```bash
   # CloudWatch alarm for KMS permission errors
   aws cloudwatch put-metric-alarm \
     --alarm-name "pii-vault-kms-access-denied" \
     --alarm-description "Alert if KMS access denied errors spike" \
     --metric-name "KMSAccessDenied" \
     --namespace "PiiVault" \
     --statistic Sum \
     --period 300 \
     --threshold 10 \
     --comparison-operator GreaterThanThreshold \
     --evaluation-periods 1
   ```

**Recommended Action:**
- Implement automated policy copy (Step 1)
- Add permission validation test (Step 3)
- Set up CloudWatch alarms (Step 5)
- Document policy requirements in version control

---

## Key Lifecycle Management

### Key States

| State | Description | Encryption | Decryption | Action |
|-------|-------------|-----------|-----------|--------|
| **ENABLED** | Active and usable | ✅ Yes | ✅ Yes | Use for new encryptions |
| **DISABLED** | Temporarily stopped | ❌ No | ✅ Yes | During rotation/maintenance |
| **PENDING DELETION** | Scheduled for removal | ❌ No | ✅ Yes | 7-30 day grace period |
| **DELETED** | Permanently removed | ❌ No | ❌ No | Cannot decrypt old data |

### Recommended Key Lifecycle

```
Day 0:  Create KeyId-1 (ENABLED)
Day 14: Create KeyId-2 (ENABLED)
        Application switches to KeyId-2
Day 28: Create KeyId-3 (ENABLED)
        Application switches to KeyId-3
Day 42: Create KeyId-4 (ENABLED)
        Application switches to KeyId-4
        DISABLE KeyId-1 (kept for decryption of old data)
Day 420: SCHEDULE DELETION KeyId-1 (7-year old key)
        30-day grace period begins
Day 450: KeyId-1 DELETED
```

### Decryption of Old Data

**Important:** Even after a key is disabled, it remains usable for decryption:

```
Encrypted data from Day 0 (encrypted with KeyId-1)
  ↓
Application finds encrypted_key_id = "KeyId-1" in metadata
  ↓
[Even though KeyId-1 is DISABLED, KMS still allows decryption]
  ↓
PII successfully decrypted
```

**This ensures:**
- Old data can always be decrypted
- No key versioning complexity in application
- Clean separation of encryption and decryption keys
- Compliance with retention policies

---

## Key Alias Management

### KMS Alias Registry Table

PII Vault maintains a **KmsKeyAlias** table to track all active and historical KMS key aliases and their ARNs.

**Table Schema:**
```sql
CREATE TABLE kms_key_alias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_alias VARCHAR(255) UNIQUE NOT NULL,
    key_arn VARCHAR(255) UNIQUE NOT NULL,
    key_id VARCHAR(255) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    activated_at TIMESTAMP,
    deactivated_at TIMESTAMP,
    reason VARCHAR(500),
    -- Audit and immutability
    created_by VARCHAR(255),
    rotations_count INT DEFAULT 0,
    last_rotation_at TIMESTAMP,
    INDEX idx_key_alias (key_alias),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at),
    INDEX idx_activated_at (activated_at),
    CONSTRAINT active_key_unique CHECK (
        CASE WHEN is_active = true THEN 1 ELSE 0 END
    )
);
```

**Column Definitions:**
- `id`: Auto-incrementing primary key
- `key_alias`: Human-readable alias (e.g., `alias/pii-vault-encryption-key-2026-07-08`)
- `key_arn`: AWS ARN of the KMS key (e.g., `arn:aws:kms:us-east-1:123456789012:key/12345678...`)
- `key_id`: KMS Key ID (short form)
- `is_active`: Boolean flag (only ONE key should be active at a time)
- `created_at`: When this key alias was created
- `activated_at`: When this key was marked as active (for encryption)
- `deactivated_at`: When this key was deactivated
- `reason`: Reason for deactivation (e.g., "Rotated to new key", "Compromised", "End of life")
- `created_by`: User/system that created the key entry
- `rotations_count`: Number of automatic KMS rotations on this key
- `last_rotation_at`: Last time AWS KMS rotated this key's internal material
- **Indexes**: key_alias (lookup), is_active (find current key), created_at (timeline), activated_at (rotation history)
- **Constraint**: Ensures only one key is marked as active

### Key Alias Lifecycle

```
Day 0:  Create alias entry
        is_active = false (standby)
        ↓
Day 1:  Activate alias (admin action)
        is_active = true (start encrypting with this key)
        activated_at = now
        ↓
Day 14: New key generated
        Previous key marked is_active = false
        New key marked is_active = true
        ↓
Day 420: Deactivate old key (7 years old)
        is_active = false
        deactivated_at = now
        reason = "Archived after retention period"
```

### Encryption with Active Key

**Application Logic:**
```java
@Service
public class EncryptionService {
    
    public EncryptedPii encryptPii(String piiValue, String piiType) {
        // 1. Get current active key from database
        KmsKeyAlias activeKey = kmsKeyAliasRepository.findByIsActive(true);
        
        if (activeKey == null) {
            throw new KmsKeyRotationException("No active KMS key found");
        }
        
        // 2. Encrypt with active key's ARN
        byte[] encryptedData = encryptWithAes256Gcm(piiValue, activeKey.getKeyArn());
        
        // 3. Store encrypted data with reference to key that was used
        EncryptedPiiData entry = new EncryptedPiiData();
        entry.setEncryptedData(encryptedData);
        entry.setKeyAliasId(activeKey.getId());  // Store which key was used
        entry.setKeyAliasUsed(activeKey.getKeyAlias());  // For audit trail
        entry.setCreatedAt(Instant.now());
        
        return encryptedPiiRepository.save(entry);
    }
    
    public String decryptPii(EncryptedPiiData encryptedEntry) {
        // 1. Get the key that was used for this specific encryption
        KmsKeyAlias keyUsed = kmsKeyAliasRepository.findById(encryptedEntry.getKeyAliasId());
        
        // 2. Decrypt using the original key (even if no longer active)
        // Works because AWS KMS allows decryption with disabled/rotated keys
        byte[] decryptedData = decryptWithAes256Gcm(
            encryptedEntry.getEncryptedData(),
            keyUsed.getKeyArn()
        );
        
        return new String(decryptedData);
    }
}
```

**Key Points:**
- Each encrypted PII record stores which key alias was used
- Decryption uses the ORIGINAL key that encrypted the data
- Disabled keys remain usable for decryption indefinitely
- No data loss from key rotation

### Querying Key Alias History

```sql
-- Find current active key
SELECT * FROM kms_key_alias WHERE is_active = true;

-- Find all keys and their activation timeline
SELECT 
    key_alias,
    created_at,
    activated_at,
    deactivated_at,
    DATEDIFF(deactivated_at, activated_at) as active_duration_days
FROM kms_key_alias
ORDER BY created_at DESC;

-- Find keys used for specific PII entries
SELECT 
    ka.key_alias,
    COUNT(*) as pii_count,
    MIN(epd.created_at) as first_used,
    MAX(epd.created_at) as last_used
FROM kms_key_alias ka
JOIN encrypted_pii_data epd ON ka.id = epd.key_alias_id
GROUP BY ka.key_alias
ORDER BY last_used DESC;

-- Audit: Who activated which key when
SELECT 
    key_alias,
    created_by,
    activated_at,
    reason
FROM kms_key_alias
WHERE is_active = false AND deactivated_at IS NOT NULL
ORDER BY deactivated_at DESC;
```

---

## Admin Endpoints for Key Management

### 1. Activate New KMS Key (Force Rotation)

**Endpoint:** `POST /api/v1/admin/kms/activate-key`

**Authentication:** Requires KMS_ADMIN role

**Request Body:**
```json
{
  "keyAlias": "alias/pii-vault-encryption-key-2026-07-22",
  "keyArn": "arn:aws:kms:us-east-1:123456789012:key/87654321-4321-4321-4321-210987654321",
  "keyId": "87654321-4321-4321-4321-210987654321",
  "reason": "Scheduled rotation",
  "requestId": "550e8400-e29b-41d4-a716-446655440100"
}
```

**Response (200 OK):**
```json
{
  "previousActiveKey": "alias/pii-vault-encryption-key-2026-07-08",
  "newActiveKey": "alias/pii-vault-encryption-key-2026-07-22",
  "activatedAt": "2026-07-22T02:00:00Z",
  "reason": "Scheduled rotation",
  "requestId": "550e8400-e29b-41d4-a716-446655440100",
  "status": "SUCCESS"
}
```

**Workflow:**
```
1. Validate new key exists in AWS KMS
2. Validate key has proper IAM permissions
3. Check new key is not already in database
4. Mark current active key as inactive
5. Mark new key as active in kms_key_alias table
6. Emit KEY_ACTIVATED event
7. Return confirmation
8. Application polls and picks up new active key (within 5 minutes)
```

**Side Effects:**
- All new PII encryptions use the new active key
- Old data remains encrypted with old keys (no re-encryption)
- Decryption of old data still works with old keys

### 2. Get Current Active Key

**Endpoint:** `GET /api/v1/admin/kms/active-key`

**Authentication:** Requires KMS_ADMIN role

**Response (200 OK):**
```json
{
  "keyAlias": "alias/pii-vault-encryption-key-2026-07-22",
  "keyArn": "arn:aws:kms:us-east-1:123456789012:key/87654321-4321-4321-4321-210987654321",
  "keyId": "87654321-4321-4321-4321-210987654321",
  "createdAt": "2026-07-22T00:00:00Z",
  "activatedAt": "2026-07-22T02:00:00Z",
  "daysActive": 0,
  "piiEntriesCount": 1250
}
```

### 3. Get Key Rotation History

**Endpoint:** `GET /api/v1/admin/kms/rotation-history`

**Authentication:** Requires KMS_ADMIN role

**Query Parameters:**
- `limit`: Max results (default: 50, max: 1000)
- `offset`: Pagination offset (default: 0)

**Response (200 OK):**
```json
{
  "keys": [
    {
      "keyAlias": "alias/pii-vault-encryption-key-2026-07-22",
      "keyArn": "arn:aws:kms:us-east-1:123456789012:key/87654321-4321-4321-4321-210987654321",
      "createdAt": "2026-07-22T00:00:00Z",
      "activatedAt": "2026-07-22T02:00:00Z",
      "deactivatedAt": null,
      "activeDurationDays": 0,
      "isActive": true,
      "piiEntriesEncryptedCount": 1250,
      "createdBy": "rotation-cron-job",
      "reason": null
    },
    {
      "keyAlias": "alias/pii-vault-encryption-key-2026-07-08",
      "keyArn": "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012",
      "createdAt": "2026-07-08T00:00:00Z",
      "activatedAt": "2026-07-08T02:00:00Z",
      "deactivatedAt": "2026-07-22T02:00:00Z",
      "activeDurationDays": 14,
      "isActive": false,
      "piiEntriesEncryptedCount": 5230,
      "createdBy": "rotation-cron-job",
      "reason": "Scheduled rotation to new key"
    }
  ],
  "totalKeys": 2,
  "currentPage": 1
}
```

### 4. Validate Key Permissions (Pre-Rotation Check)

**Endpoint:** `POST /api/v1/admin/kms/validate-key`

**Authentication:** Requires KMS_ADMIN role

**Request Body:**
```json
{
  "keyArn": "arn:aws:kms:us-east-1:123456789012:key/87654321-4321-4321-4321-210987654321",
  "requestId": "550e8400-e29b-41d4-a716-446655440101"
}
```

**Response (200 OK):**
```json
{
  "keyArn": "arn:aws:kms:us-east-1:123456789012:key/87654321-4321-4321-4321-210987654321",
  "isValid": true,
  "canEncrypt": true,
  "canDecrypt": true,
  "keyState": "Enabled",
  "hasCorrectPolicy": true,
  "validationTests": {
    "encryptTest": "PASS",
    "decryptTest": "PASS",
    "policyTest": "PASS"
  },
  "requestId": "550e8400-e29b-41d4-a716-446655440101",
  "testedAt": "2026-07-22T02:30:00Z"
}
```

---

## Future Feature: Key Decommissioning with Data Migration

### Phase 4: Seamless Key Migration

**Goal:** Migrate all PII encrypted with KeyA to KeyB without user impact.

**Workflow:**
```
Admin Action: Decommission KeyA → Migrate to KeyB
  ↓
1. Create new KeyB (already have this)
  ↓
2. Background job reads all PII encrypted with KeyA
  ↓
3. For each PII entry:
   - Decrypt using KeyA
   - Re-encrypt using KeyB
   - Update encrypted_pii_data record
  ↓
4. Update metadata to reference KeyB
  ↓
5. Mark KeyA as decommissioned
  ↓
6. Schedule KeyA for deletion
```

**Database Changes:**
```sql
ALTER TABLE encrypted_pii_data ADD COLUMN 
    migration_status VARCHAR(50) DEFAULT 'ACTIVE'
    -- Values: ACTIVE, PENDING_MIGRATION, MIGRATED

ALTER TABLE kms_key_alias ADD COLUMN
    migration_target_key_id BIGINT NULL
    -- References the key this one is being migrated to
    
ALTER TABLE kms_key_alias ADD COLUMN
    decommissioning_progress INT DEFAULT 0
    -- Percentage: 0-100
```

**Admin Endpoint (Phase 4):**
```
POST /api/v1/admin/kms/decommission-key

Request:
{
  "sourceKeyId": "12345678-1234-1234-1234-123456789012",
  "targetKeyId": "87654321-4321-4321-4321-210987654321",
  "batchSize": 1000,
  "requestId": "550e8400-e29b-41d4-a716-446655440102"
}

Response (202 Accepted):
{
  "migrationId": "migration-001",
  "sourceKey": "alias/pii-vault-encryption-key-2026-01-01",
  "targetKey": "alias/pii-vault-encryption-key-2026-07-22",
  "totalPiiEntries": 125000,
  "progressPercentage": 0,
  "status": "INITIATED",
  "estimatedTimeMinutes": 45,
  "requestId": "550e8400-e29b-41d4-a716-446655440102"
}

GET /api/v1/admin/kms/decommission-key/{migrationId}

Response:
{
  "migrationId": "migration-001",
  "sourceKey": "alias/pii-vault-encryption-key-2026-01-01",
  "targetKey": "alias/pii-vault-encryption-key-2026-07-22",
  "totalPiiEntries": 125000,
  "migratedCount": 45000,
  "progressPercentage": 36,
  "status": "IN_PROGRESS",
  "startedAt": "2026-07-22T03:00:00Z",
  "estimatedCompletionTime": "2026-07-22T03:45:00Z"
}
```

**Key Features:**
- Non-blocking background migration (doesn't impact encryption/decryption)
- Progress tracking and monitoring
- Pause/resume capability
- Rollback if needed
- Parallel re-encryption for performance
- Automatic verification after migration

---

## Configuration Management

### Application Configuration

**Spring Boot Configuration (application.yml):**
```yaml
pii-vault:
  kms:
    enabled: true
    region: us-east-1
    current-key-id: ${AWS_KMS_KEY_ID}  # Injected from Parameter Store
    key-cache-ttl: 3600                 # Cache KMS calls (1 hour)
    rotation-schedule: "0 2 */14 * *"   # Every 14 days at 2 AM UTC
    
  encryption:
    algorithm: AES_256_GCM
    key-derivation: HKDF-SHA256
```

### AWS Systems Manager Parameter Store

Store the current active KMS key ID in Parameter Store for easy updates without application restart:

```bash
# Create parameter
aws ssm put-parameter \
  --name "/pii-vault/kms/current-key-id" \
  --value "arn:aws:kms:us-east-1:123456789012:key/12345678-1234-1234-1234-123456789012" \
  --type "String" \
  --description "Current active KMS key for PII Vault encryption"

# Retrieve parameter
aws ssm get-parameter \
  --name "/pii-vault/kms/current-key-id" \
  --query 'Parameter.Value' \
  --output text

# Update on key rotation (done by cron job)
aws ssm put-parameter \
  --name "/pii-vault/kms/current-key-id" \
  --value "$NEW_KEY_ID" \
  --overwrite
```

**Benefits:**
- No application redeploy needed for key rotation
- Centralized key management
- Audit trail via Parameter Store history
- Can update immediately without container restart

### Monitoring the Current Key

Application polls Parameter Store every 5 minutes:

```java
@Component
public class KmsKeyRotationService {
    
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void refreshCurrentKeyId() {
        String currentKeyId = parameterStore.getParameter("/pii-vault/kms/current-key-id");
        if (!currentKeyId.equals(this.activeKeyId)) {
            logger.info("KMS key rotated: {} → {}", this.activeKeyId, currentKeyId);
            this.activeKeyId = currentKeyId;
            // Warm up key cache
            kmsClient.describeKey(currentKeyId);
        }
    }
}
```

---

## Monitoring and Alerting

### Key Metrics

```
✓ Active KMS Key Count
✓ Key Rotation Events (via CloudTrail)
✓ KMS Encrypt/Decrypt Latency
✓ KMS API Throttling Events
✓ Access Denied Errors
✓ Key State Changes (enable/disable/delete)
```

### CloudWatch Dashboard

```bash
aws cloudwatch put-dashboard \
  --dashboard-name pii-vault-kms \
  --dashboard-body file://kms-dashboard.json
```

**Dashboard Contents:**
- Current active key ID
- Key creation events (timeline)
- KMS API latency
- Encryption/decryption success rate
- Access denied errors
- CloudTrail events

### Alerts (Critical)

1. **KMS Access Denied** (threshold: 1 error)
   - Indicates IAM policy issue on new key
   - Requires immediate investigation

2. **KMS Throttling** (threshold: 5 errors in 5 min)
   - Too many KMS API calls
   - May need caching or batching adjustments

3. **Key Rotation Failed** (threshold: 1 failure)
   - Cron job failed or script error
   - Check logs and manually verify key creation

4. **Old Key Disabled Without Backup** (threshold: N/A)
   - Manual validation before disabling keys
   - Ensure all new data uses new key first

---

## Testing and Validation

### Pre-Rotation Testing

Before deploying rotation automation, test the entire flow:

```bash
#!/bin/bash
# test-kms-rotation.sh

echo "1. Create test key..."
TEST_KEY=$(aws kms create-key --description "Test rotation" \
  --key-policy file://test-policy.json --query 'KeyMetadata.KeyId' --output text)

echo "2. Test encryption with new key..."
PLAINTEXT="test-data-12345"
ENCRYPTED=$(aws kms encrypt --key-id "$TEST_KEY" --plaintext "$PLAINTEXT" \
  --query 'CiphertextBlob' --output text)

echo "3. Test decryption with new key..."
DECRYPTED=$(aws kms decrypt --ciphertext-blob "$ENCRYPTED" \
  --query 'Plaintext' --output text | base64 -d)

if [[ "$PLAINTEXT" == "$DECRYPTED" ]]; then
  echo "✓ Encryption/decryption working"
else
  echo "✗ Test failed"
  exit 1
fi

echo "4. Schedule test key for deletion..."
aws kms schedule-key-deletion --key-id "$TEST_KEY" --pending-window-in-days 7

echo "✓ All tests passed"
```

### Post-Rotation Validation

After each rotation:

```bash
#!/bin/bash
# validate-kms-rotation.sh

NEW_KEY=$1

echo "Validating KMS key rotation..."

# 1. Verify key exists and is enabled
STATE=$(aws kms describe-key --key-id "$NEW_KEY" \
  --query 'KeyMetadata.KeyState' --output text)
[[ "$STATE" == "Enabled" ]] || exit 1

# 2. Test encrypt/decrypt cycle
TEST=$(aws kms encrypt --key-id "$NEW_KEY" --plaintext "test" \
  --query 'CiphertextBlob' --output text | \
  xargs -I {} aws kms decrypt --ciphertext-blob {} \
  --query 'Plaintext' --output text | base64 -d)
[[ "$TEST" == "test" ]] || exit 1

# 3. Verify key policy contains app role
POLICY=$(aws kms get-key-policy --key-id "$NEW_KEY" \
  --policy-name default --output text)
grep -q "pii-vault-app-role" <<< "$POLICY" || exit 1

echo "✓ KMS key rotation validated successfully"
```

---

## Recovery Procedures

### Scenario: KMS Key Policy Missing Permissions

**Symptoms:**
- "KMS AccessDenied" errors in logs
- Encryption/decryption operations failing
- Recently deployed new KMS key

**Recovery Steps:**
```bash
# 1. Identify problematic key
PROBLEM_KEY=$(grep -o "arn:aws:kms:[^ ]*" /var/log/pii-vault/error.log | head -1)

# 2. Get correct policy from previous key
PREVIOUS_KEY=$(aws ssm get-parameter --name "/pii-vault/kms/previous-key-id" \
  --query 'Parameter.Value' --output text)
aws kms get-key-policy --key-id "$PREVIOUS_KEY" \
  --policy-name default > correct-policy.json

# 3. Apply correct policy to problematic key
aws kms put-key-policy --key-id "$PROBLEM_KEY" \
  --policy-name default --policy file://correct-policy.json

# 4. Validate fix
sleep 5
aws kms encrypt --key-id "$PROBLEM_KEY" \
  --plaintext "test" && echo "✓ Fixed"

# 5. Switch back to repaired key
aws ssm put-parameter --name "/pii-vault/kms/current-key-id" \
  --value "$PROBLEM_KEY" --overwrite

# 6. Restart application to refresh cache
systemctl restart pii-vault
```

### Scenario: Need to Decrypt Data from Disabled Key

**Symptoms:**
- Data was encrypted with an old, disabled key
- Need to decrypt that data

**Solution:**
KMS allows decryption from disabled keys without any additional action:

```bash
# Even with key disabled, this works:
aws kms decrypt \
  --ciphertext-blob $ENCRYPTED_DATA \
  --key-id arn:aws:kms:region:account:key/disabled-key-id
# Returns: Success
```

No action needed - disabled keys remain usable for decryption.

---

## Implementation Roadmap

### Phase 1 (v1.0): Single Key
- Deploy single customer-managed KMS key
- Manual key management
- Basic CloudTrail logging

### Phase 2 (v1.1): Automated Rotation (N-day)
- Implement cron-based key creation
- Automated policy copy
- Parameter Store integration
- CloudWatch monitoring
- Initial N = 30 days

### Phase 3 (v1.2): Optimized Rotation (N=14 days)
- Reduce rotation interval to 14 days
- Add comprehensive validation
- Implement gradual migration strategy

### Phase 4 (v2.0): Multi-Region
- Replicate keys across regions
- Regional failover
- Cross-region decryption support

---

## References and Best Practices

### AWS KMS Best Practices
- [AWS KMS Best Practices](https://docs.aws.amazon.com/kms/latest/developerguide/best-practices.html)
- [KMS Key Rotation](https://docs.aws.amazon.com/kms/latest/developerguide/rotating-keys.html)
- [CloudTrail Logging for KMS](https://docs.aws.amazon.com/kms/latest/developerguide/logging-using-cloudtrail.html)

### Related Documentation
- See [PRD.md](./PRD.md) for product requirements
- See [API.md](./API.md) for encryption/decryption endpoints

---

**Last Updated**: 2026-07-08
**Version**: 1.0
**Status**: Ready for Phase 2 Implementation

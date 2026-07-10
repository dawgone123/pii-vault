# KMS Implementation Guide - Phase 1 to Phase 3

**Document Date**: July 2026  
**Approach**: KMS-Only Encryption (All Phases)  
**No Local Crypto**: Application delegates all encryption to KMS

---

## Quick Summary

| Phase | Environment | KMS Backend | Code Changes | Config Changes |
|-------|-------------|-------------|--------------|-----------------|
| **Phase 1** | Local development | LocalStack KMS mock | ❌ None | ✅ endpoint-url: localstack:4566 |
| **Phase 2** | Production (AWS) | AWS KMS | ❌ None | ✅ endpoint-url: AWS (IAM role) |
| **Phase 3** | Multi-region | AWS KMS multi-region | ❌ None | ✅ key-alias with region replicas |

**Key Principle**: Same Java code runs everywhere. Only configuration changes.

---

## Phase 1: LocalStack KMS Mock

### Docker Compose Setup

```yaml
version: '3.9'

services:
  
  # LocalStack provides AWS KMS mock
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"  # LocalStack edge API
    environment:
      - SERVICES=kms,s3,logs
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=us-east-1
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - pii-vault

  # PII Vault application
  pii-vault:
    build: .
    ports:
      - "8080:8080"
    environment:
      # AWS Credentials (LocalStack accepts any values)
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_REGION=us-east-1
      
      # KMS Configuration
      - AWS_ENDPOINT_URL_KMS=http://localstack:4566
      - KMS_KEY_ALIAS=alias/pii-vault-dev
      - ENCRYPTION_MODE=kms
      
      # Database
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pii_vault
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      
      # Cache & Observability
      - REDIS_HOST=redis
      - METRICS_EXPORT_VICTORIAMETRICS_URL=http://victoriametrics:8428
    
    depends_on:
      - localstack
      - postgres
      - redis
    
    networks:
      - pii-vault

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=pii_vault
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - pii-vault

  redis:
    image: redis:7-alpine
    networks:
      - pii-vault

  victoriametrics:
    image: victoriametrics/victoria-metrics:latest
    ports:
      - "8428:8428"
    networks:
      - pii-vault

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    networks:
      - pii-vault

networks:
  pii-vault:
    driver: bridge

volumes:
  postgres_data:
```

### Spring Boot Configuration (Phase 1)

```yaml
# application.yml
spring:
  application:
    name: pii-vault

# AWS Configuration
aws:
  region: us-east-1
  endpoint-url: ${AWS_ENDPOINT_URL_KMS:http://localhost:4566}

# KMS Configuration
encryption:
  mode: kms  # Use KMS (not local crypto)
  kms:
    enabled: true
    endpoint-url: ${AWS_ENDPOINT_URL_KMS:http://localhost:4566}
    region: us-east-1
    key-alias: alias/pii-vault-dev
    key-rotation-enabled: false  # LocalStack doesn't support rotation
```

### Application Code (Phase 1)

```java
@Service
public class KmsEncryptionService {
  
  @Autowired
  private KmsClient kmsClient;
  
  @Value("${encryption.kms.key-alias}")
  private String keyAlias;
  
  /**
   * Encrypt PII via KMS
   * Works with both LocalStack (Phase 1) and AWS KMS (Phase 2+)
   */
  public String encrypt(String plaintext) {
    try {
      EncryptRequest request = EncryptRequest.builder()
        .keyId(keyAlias)
        .plaintext(SdkBytes.fromUtf8String(plaintext))
        .build();
      
      EncryptResponse response = kmsClient.encrypt(request);
      return response.ciphertextBlob().asUtf8String();
      
    } catch (KmsException e) {
      throw new EncryptionException("KMS encrypt failed: " + e.getMessage());
    }
  }
  
  /**
   * Decrypt PII via KMS
   * Works with both LocalStack (Phase 1) and AWS KMS (Phase 2+)
   */
  public String decrypt(String ciphertext) {
    try {
      DecryptRequest request = DecryptRequest.builder()
        .ciphertextBlob(SdkBytes.fromUtf8String(ciphertext))
        .build();
      
      DecryptResponse response = kmsClient.decrypt(request);
      return response.plaintext().asUtf8String();
      
    } catch (KmsException e) {
      throw new DecryptionException("KMS decrypt failed: " + e.getMessage());
    }
  }
}

// Zero local crypto implementation
// All encryption/decryption delegated to KMS
```

### LocalStack KMS Mock Behavior

LocalStack emulates AWS KMS API exactly:
- ✅ Same endpoint: `http://localstack:4566`
- ✅ Same SDK: `software.amazon.awssdk.services.kms.KmsClient`
- ✅ Same request/response format
- ✅ Accepts any AWS credentials (test/test)
- ✅ Creates keys on-demand (no setup needed)

**Differences from production**:
- Encryption is simulated (not truly encrypted)
- No CloudTrail logging
- No key rotation
- No multi-region support
- Not FIPS-certified (obviously)

**But**: API contract is identical → Same code works in Phase 2

### Testing with LocalStack KMS

```java
@SpringBootTest
@ActiveProfiles("test")
public class KmsIntegrationTest {
  
  @Autowired
  private KmsEncryptionService encryptionService;
  
  @Test
  public void testEncryptDecryptRoundtrip() {
    String original = "sensitive-data@example.com";
    
    // Encrypt via LocalStack KMS
    String encrypted = encryptionService.encrypt(original);
    assertThat(encrypted).isNotEmpty();
    assertThat(encrypted).isNotEqualTo(original);
    
    // Decrypt via LocalStack KMS
    String decrypted = encryptionService.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(original);
  }
  
  @Test
  public void testKmsAlias() {
    // LocalStack creates alias/pii-vault-dev automatically
    // Just like AWS KMS
    String encrypted = encryptionService.encrypt("test");
    assertThat(encrypted).isNotEmpty();
  }
}
```

### Startup Checklist (Phase 1)

```bash
# 1. Start LocalStack KMS mock
docker-compose up localstack

# 2. Wait for LocalStack to be ready
# LocalStack should print: "Ready." to stdout

# 3. Verify LocalStack KMS is responding
curl http://localhost:4566/health

# Expected output: {"services": {"kms": "running"}}

# 4. Start PII Vault
docker-compose up pii-vault

# 5. Verify KMS encryption works
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{"piiData": "test@example.com"}'

# Expected: encrypted token
```

---

## Phase 2: AWS KMS Production

### Transition from Phase 1 → Phase 2

**Code Changes**: ZERO
**Configuration Changes Only**:

```yaml
# application.yml (Phase 2 - Production)
aws:
  region: us-east-1  # or us-west-2, etc.
  # NO endpoint-url → Uses real AWS KMS

encryption:
  mode: kms
  kms:
    enabled: true
    # NO endpoint-url → Uses real AWS KMS
    region: us-east-1
    key-alias: alias/pii-vault-prod  # Different alias
    key-rotation-enabled: true  # Enable annual rotation
    key-rotation-days: 365
```

### AWS KMS Setup (Phase 2)

```bash
# Create KMS key via AWS CLI
aws kms create-key \
  --description "PII Vault Encryption Key" \
  --region us-east-1

# Create alias
aws kms create-alias \
  --alias-name alias/pii-vault-prod \
  --target-key-id <key-id>

# Enable automatic key rotation
aws kms enable-key-rotation \
  --key-id alias/pii-vault-prod
```

Or via Terraform (Phase 3):

```hcl
# terraform/kms.tf
resource "aws_kms_key" "pii_vault" {
  description             = "PII Vault encryption key"
  deletion_window_in_days = 30
  enable_key_rotation     = true  # Annual rotation
  
  tags = {
    Name = "pii-vault-key"
  }
}

resource "aws_kms_alias" "pii_vault" {
  name          = "alias/pii-vault-prod"
  target_key_id = aws_kms_key.pii_vault.key_id
}
```

### Spring Boot Configuration (Phase 2)

```yaml
# application.yml (Phase 2)
spring:
  application:
    name: pii-vault

# AWS Configuration (uses IAM role, no access keys)
aws:
  region: ${AWS_REGION:us-east-1}
  # No endpoint-url → Uses production AWS KMS

encryption:
  mode: kms
  kms:
    enabled: true
    region: ${AWS_REGION:us-east-1}
    key-alias: ${KMS_KEY_ALIAS:alias/pii-vault-prod}
    key-rotation-enabled: true
    key-rotation-days: 365
```

### Application Code (Phase 2)

No changes! Same Java code as Phase 1:

```java
// EncryptionService.java - IDENTICAL CODE
@Service
public class KmsEncryptionService {
  
  @Autowired
  private KmsClient kmsClient;
  
  public String encrypt(String plaintext) {
    // Same code as Phase 1
    // But now uses production AWS KMS
    // CloudTrail logs all operations
  }
}
```

### AWS Credentials (Phase 2)

Spring Boot automatically uses IAM role:

```java
@Configuration
public class AwsConfig {
  
  @Bean
  public KmsClient kmsClient(AwsProperties props) {
    // AWS SDK automatically uses IAM role
    // No credentials in code
    return KmsClient.builder()
      .region(Region.of(props.getRegion()))
      .build();
  }
}
```

IAM Role required:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Encrypt",
        "kms:Decrypt",
        "kms:GenerateDataKey",
        "kms:DescribeKey"
      ],
      "Resource": "arn:aws:kms:*:ACCOUNT:key/*"
    }
  ]
}
```

### CloudTrail Logging (Phase 2)

AWS KMS automatically logs to CloudTrail:

```bash
# View KMS operations in CloudTrail
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=ResourceName,AttributeValue=alias/pii-vault-prod \
  --region us-east-1

# Shows:
# - Encrypt operations
# - Decrypt operations
# - Key management operations
# - Who performed operation
# - When operation occurred
# - Result (success/failure)
```

---

## Phase 3: Multi-Region KMS

### Multi-Region Key Replication

```hcl
# terraform/kms.tf (Phase 3)

# Primary region key
resource "aws_kms_key" "pii_vault_primary" {
  description             = "PII Vault primary key"
  enable_key_rotation     = true
  region                  = "us-east-1"
}

# Secondary region replica
resource "aws_kms_replica_key" "pii_vault_secondary" {
  provider        = aws.secondary
  primary_key_id  = aws_kms_key.pii_vault_primary.id
  description     = "PII Vault secondary key replica"
  region          = "us-west-2"
}
```

### Application Code (Phase 3)

No changes! Multi-region KMS is transparent:

```java
// EncryptionService.java - IDENTICAL CODE
// Works with primary key in us-east-1
// Automatically fails over to secondary key in us-west-2
```

Configuration adjusts by region:

```yaml
# application.yml (Phase 3 - Multi-region)
aws:
  region: ${AWS_REGION}  # us-east-1 or us-west-2

encryption:
  kms:
    region: ${AWS_REGION}
    key-alias: alias/pii-vault-prod
    key-rotation-enabled: true
```

---

## Complete Phase Comparison

### Phase 1 (Development)

| Component | Implementation |
|-----------|-----------------|
| **KMS** | LocalStack mock (docker-compose) |
| **Encryption** | Simulated (LocalStack) |
| **Audit** | None (LocalStack doesn't log) |
| **Key Rotation** | Not supported (LocalStack) |
| **Multi-region** | Not supported (LocalStack) |
| **Code** | KMS-based (single encrypt/decrypt) |
| **Configuration** | `endpoint-url: http://localstack:4566` |
| **Credentials** | test/test |
| **Testing** | Integration tests with LocalStack |

### Phase 2 (Production - Single Region)

| Component | Implementation |
|-----------|-----------------|
| **KMS** | AWS KMS (production) |
| **Encryption** | AES-256-GCM (hardware accelerated) |
| **Audit** | CloudTrail logging enabled |
| **Key Rotation** | Annual automatic rotation |
| **Multi-region** | Not configured |
| **Code** | IDENTICAL to Phase 1 |
| **Configuration** | No endpoint-url (uses AWS) |
| **Credentials** | IAM role (no secrets) |
| **Testing** | Integration tests with AWS KMS |

### Phase 3 (Production - Multi-Region)

| Component | Implementation |
|-----------|-----------------|
| **KMS** | AWS KMS (multi-region replicas) |
| **Encryption** | AES-256-GCM with failover |
| **Audit** | CloudTrail logging (all regions) |
| **Key Rotation** | Annual rotation (auto-replicated) |
| **Multi-region** | Replicated across us-east-1, us-west-2 |
| **Code** | IDENTICAL to Phase 1 & 2 |
| **Configuration** | Multi-region deployment |
| **Credentials** | IAM role (cross-region) |
| **Testing** | Multi-region failover tests |

---

## Why KMS-Only (Architecture Benefits)

### Simplicity
- ✅ One encryption mechanism (no AES-256-GCM code)
- ✅ No IV/auth tag management in app
- ✅ No key derivation logic
- ✅ Just encrypt/decrypt calls

### Security
- ✅ No plaintext keys in code
- ✅ FIPS 140-2 compliant (AWS KMS)
- ✅ Hardware-accelerated crypto
- ✅ Automatic key rotation
- ✅ Complete audit trail (CloudTrail)

### Testability
- ✅ LocalStack KMS behaves identically to AWS
- ✅ Same code runs locally and in production
- ✅ No need for separate crypto tests
- ✅ Integration tests cover everything

### Operations
- ✅ AWS handles key rotation
- ✅ Multi-region failover automatic
- ✅ No crypto library maintenance
- ✅ Compliance is AWS's responsibility

---

## Troubleshooting

### LocalStack Not Connecting (Phase 1)

```bash
# Check LocalStack is running
docker-compose logs localstack

# Should see: "Ready."

# Test endpoint
curl http://localhost:4566/health

# If not working:
# 1. Stop containers: docker-compose down
# 2. Remove volumes: docker volume prune
# 3. Restart: docker-compose up
```

### AWS KMS Permission Denied (Phase 2)

```bash
# Verify IAM role has KMS permissions
aws sts get-caller-identity

# Check KMS key access
aws kms describe-key --key-id alias/pii-vault-prod

# Add IAM permissions if needed
# See IAM policy example above
```

### Multi-Region Replication Lag (Phase 3)

```bash
# Check key replication status
aws kms describe-key --key-id alias/pii-vault-prod --region us-east-1

# Should show:
# "MultiRegion": true
# "PrimaryRegion": "us-east-1"

# Check secondary replica
aws kms describe-key --key-id alias/pii-vault-prod --region us-west-2
```

---

## Summary

**One codebase, three environments**:

```
Code: Same EncryptionService.java
      ├─ Phase 1: LocalStack KMS mock (config: endpoint-url=localhost:4566)
      ├─ Phase 2: AWS KMS (config: region=us-east-1, key-alias=prod)
      └─ Phase 3: AWS KMS multi-region (config: multi-region-replication)
```

**Key benefits**:
- ✅ Simpler architecture (no local crypto)
- ✅ Locally testable (LocalStack)
- ✅ Production-ready immediately
- ✅ Better security (FIPS 140-2)
- ✅ Better compliance (CloudTrail audit)
- ✅ No configuration drift (same code)

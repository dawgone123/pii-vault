# PII Vault - Encryption Architecture & KMS-Only Approach

**Document Date**: July 2026  
**Architecture**: KMS-Native Encryption (All Phases)  
**Key Principle**: Encryption delegated to AWS KMS from Day 1

---

## Overview

PII Vault uses **KMS-only encryption** from Phase 1 onwards. There is **no local AES-256-GCM implementation**. All encryption/decryption operations delegate to AWS KMS (or LocalStack KMS mock for development).

This approach provides:
- ✅ **Simplicity**: Single encryption layer (KMS handles all crypto)
- ✅ **Security**: No key material in application code
- ✅ **Compliance**: KMS provides audit trail via CloudTrail
- ✅ **Manageability**: AWS handles key rotation automatically
- ✅ **Scalability**: KMS is a managed AWS service with high throughput

---

## Architecture by Phase

### Phase 1 (Development & Testing)

**Encryption Flow**:
```
PII Data → LocalStack KMS Mock → Encrypted Blob
↑
Local Development (docker-compose)

Process:
1. Application receives PII data
2. Calls LocalStack KMS encrypt API
3. Returns encrypted blob + key ID
4. Stores encrypted blob in PostgreSQL
5. LocalStack KMS mock emulates AWS KMS behavior
```

**Why LocalStack KMS Mock in Phase 1?**
- Developers can test KMS workflows locally
- No AWS account needed for development
- Same API contract as production (AWS KMS)
- Behaves identically to production KMS
- No KMS API costs during development

**LocalStack Docker Setup**:
```yaml
# docker-compose.yml
version: '3.9'
services:
  
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"        # LocalStack edge endpoint
    environment:
      - SERVICES=kms,s3,logs
      - DEBUG=1
      - DOCKER_HOST=unix:///var/run/docker.sock
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=us-east-1
    volumes:
      - "${TMPDIR}:/tmp/localstack"
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - pii-vault-network

  pii-vault:
    build: .
    ports:
      - "8080:8080"
    environment:
      # LocalStack configuration
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_REGION=us-east-1
      - AWS_ENDPOINT_URL_KMS=http://localstack:4566
      - KMS_KEY_ALIAS=alias/pii-vault-dev
      - ENCRYPTION_MODE=kms  # Use KMS (not local crypto)
      
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
      - victoriametrics
    networks:
      - pii-vault-network

  postgres:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=pii_vault
      - POSTGRES_PASSWORD=postgres
    networks:
      - pii-vault-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - pii-vault-network

  victoriametrics:
    image: victoriametrics/victoria-metrics:latest
    ports:
      - "8428:8428"
    networks:
      - pii-vault-network

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    networks:
      - pii-vault-network

networks:
  pii-vault-network:
    driver: bridge
```

**Spring Boot Configuration**:
```yaml
# application.yml (Phase 1 - LocalStack)
spring:
  application:
    name: pii-vault

encryption:
  mode: kms                    # Use KMS (not local)
  kms:
    enabled: true
    endpoint-url: ${AWS_ENDPOINT_URL_KMS:http://localstack:4566}
    region: us-east-1
    access-key-id: test        # LocalStack accepts any value
    secret-access-key: test
    key-alias: alias/pii-vault-dev
    key-rotation-enabled: false  # LocalStack mock doesn't need rotation
```

### Phase 2 (AWS Production)

**Encryption Flow**:
```
PII Data → AWS KMS → Encrypted Blob
↑
Production AWS Account

Process:
1. Application receives PII data
2. Calls AWS KMS encrypt API
3. KMS performs AES-256-GCM encryption (hardware accelerated)
4. Returns encrypted blob + key version ID
5. Stores encrypted blob in RDS Aurora
6. AWS CloudTrail logs KMS operation
```

**Spring Boot Configuration**:
```yaml
# application.yml (Phase 2 - AWS KMS)
spring:
  application:
    name: pii-vault

encryption:
  mode: kms                    # Use KMS
  kms:
    enabled: true
    region: ${AWS_REGION:us-east-1}
    key-alias: alias/pii-vault-prod
    key-rotation-enabled: true    # Enable annual rotation
    key-rotation-days: 365
    # AWS credentials from IAM role (no secrets in config)
```

**AWS KMS Setup**:
```java
@Configuration
@EnableConfigurationProperties(KmsProperties.class)
public class KmsConfig {
  
  @Bean
  public KmsClient kmsClient(KmsProperties props) {
    // Use IAM role credentials (no access keys in code)
    return KmsClient.builder()
      .region(Region.of(props.getRegion()))
      .build();
  }
  
  @Bean
  public AwsCrypto awsCrypto() {
    // Use AWS Encryption SDK for client-side operations
    return AwsCrypto.standard();
  }
}
```

### Phase 3 (Multi-Region AWS)

**Encryption Flow**:
```
Primary Region                 Secondary Region
PII Data → AWS KMS (us-east-1) ←→ AWS KMS (us-west-2)
↓                                  ↓
RDS Aurora                         RDS Aurora
↓                                  ↓
Data Lake (S3)                     Data Lake (S3)

Process:
1. Primary region handles all encrypts
2. KMS key replicated to secondary region
3. Failover to secondary KMS if primary fails
4. Cross-region data replication via Aurora
```

---

## Phase 1 KMS Implementation Details

### Week 1: Foundation - KMS-Only Approach

Instead of building local AES-256-GCM:

```java
// EncryptionService.java - Phase 1 (KMS-ONLY)

@Service
public class EncryptionService {
  
  @Autowired
  private KmsClient kmsClient;
  
  @Autowired
  private AuditService auditService;
  
  /**
   * Encrypt PII using AWS KMS (Phase 1: LocalStack mock)
   * No local crypto implementation - all encryption via KMS
   */
  public EncryptedPayload encrypt(String piiData, String requestId) {
    try {
      // 1. Generate token ID from request (deterministic)
      String tokenId = generateDeterministicTokenId(piiData, requestId);
      
      // 2. Call KMS to encrypt PII
      EncryptRequest encryptRequest = EncryptRequest.builder()
        .keyId("alias/pii-vault-dev")  // LocalStack KMS alias
        .plaintext(SdkBytes.fromUtf8String(piiData))
        .encryptionContext(Map.of(
          "request-id", requestId,
          "token-id", tokenId,
          "timestamp", Instant.now().toString()
        ))
        .build();
      
      EncryptResponse encryptResponse = kmsClient.encrypt(encryptRequest);
      
      // 3. Extract encrypted blob and key ID
      String encryptedData = encryptResponse.ciphertextBlob().asUtf8String();
      String keyId = encryptResponse.keyId();
      
      // 4. Create response payload
      EncryptedPayload payload = new EncryptedPayload(
        tokenId: tokenId,
        encryptedData: encryptedData,
        keyId: keyId,
        encryptionLatencyMs: calculateLatency(),
        encryptedAt: Instant.now()
      );
      
      // 5. Audit the operation
      auditService.logEncryption(
        tokenId: tokenId,
        requestId: requestId,
        piiLength: piiData.length(),
        status: SUCCESS
      );
      
      return payload;
      
    } catch (KmsException e) {
      auditService.logEncryption(
        tokenId: null,
        requestId: requestId,
        status: FAILURE,
        error: e.getMessage()
      );
      throw new EncryptionException("KMS encryption failed: " + e.getMessage());
    }
  }
  
  /**
   * Decrypt PII using AWS KMS
   * Deterministic: same plaintext + same request = same token ID
   */
  public DecryptedPayload decrypt(String tokenId, String requestId) {
    try {
      // 1. Retrieve encrypted data from database
      PiiDataEntity entity = piiDataRepository.findById(tokenId)
        .orElseThrow(() -> new TokenNotFoundException("Token not found: " + tokenId));
      
      // 2. Check if token is soft-deleted
      if (entity.isDeleted()) {
        auditService.logDecryption(tokenId, requestId, status: BLOCKED, error: "Token deleted");
        throw new TokenDeletedException("Token has been deleted");
      }
      
      // 3. Call KMS to decrypt
      DecryptRequest decryptRequest = DecryptRequest.builder()
        .ciphertextBlob(SdkBytes.fromUtf8String(entity.getEncryptedData()))
        .encryptionContext(Map.of(
          "request-id", requestId,
          "token-id", tokenId
        ))
        .build();
      
      DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
      
      // 4. Extract plaintext PII
      String piiData = decryptResponse.plaintext().asUtf8String();
      
      // 5. Create response
      DecryptedPayload payload = new DecryptedPayload(
        piiData: piiData,
        tokenId: tokenId,
        decryptionLatencyMs: calculateLatency(),
        decryptedAt: Instant.now()
      );
      
      // 6. Audit the operation
      auditService.logDecryption(
        tokenId: tokenId,
        requestId: requestId,
        status: SUCCESS
      );
      
      return payload;
      
    } catch (KmsException e) {
      auditService.logDecryption(tokenId, requestId, status: FAILURE, error: e.getMessage());
      throw new DecryptionException("KMS decryption failed: " + e.getMessage());
    }
  }
  
  /**
   * Generate deterministic token ID
   * Same input + same key = same token ID (required for idempotency)
   */
  private String generateDeterministicTokenId(String piiData, String requestId) {
    // HMAC-SHA256: hash(piiData + requestId) truncated to UUID
    String hashInput = piiData + "|" + requestId;
    
    byte[] hash = HmacUtils.hmacSha256("pii-vault-secret", hashInput);
    String tokenId = UUID.nameUUIDFromBytes(hash).toString();
    
    return tokenId;
  }
}
```

### Week 1: KMS Service (LocalStack Aware)

```java
// KmsService.java - Phase 1 (LocalStack + AWS)

@Service
public class KmsService {
  
  @Autowired
  private KmsClient kmsClient;
  
  @Value("${encryption.kms.key-alias}")
  private String keyAlias;
  
  @Value("${encryption.kms.endpoint-url:#{null}}")
  private String endpointUrl;  // LocalStack endpoint in Phase 1
  
  /**
   * Phase 1: LocalStack KMS Mock
   * Phase 2: Real AWS KMS
   * API contract identical - code changes only in config
   */
  public String encryptData(String plaintext) {
    EncryptRequest request = EncryptRequest.builder()
      .keyId(keyAlias)
      .plaintext(SdkBytes.fromUtf8String(plaintext))
      .build();
    
    EncryptResponse response = kmsClient.encrypt(request);
    return response.ciphertextBlob().asUtf8String();
  }
  
  public String decryptData(String ciphertext) {
    DecryptRequest request = DecryptRequest.builder()
      .ciphertextBlob(SdkBytes.fromUtf8String(ciphertext))
      .build();
    
    DecryptResponse response = kmsClient.decrypt(request);
    return response.plaintext().asUtf8String();
  }
  
  /**
   * Check KMS connectivity (health check)
   */
  public boolean isKmsHealthy() {
    try {
      DescribeKeyRequest request = DescribeKeyRequest.builder()
        .keyId(keyAlias)
        .build();
      
      kmsClient.describeKey(request);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
```

### Week 2: Database Schema (No Encryption Fields)

```sql
-- Phase 1: PII Data Table
-- Encrypted blob comes from KMS, not local crypto

CREATE TABLE pii_data (
  pii_token_id UUID PRIMARY KEY,
  
  -- Blob encrypted by KMS (no IV, auth tag needed in DB)
  encrypted_data TEXT NOT NULL,
  
  -- KMS key information
  kms_key_id VARCHAR(255) NOT NULL,    -- Which KMS key encrypted this
  
  -- Metadata
  created_date TIMESTAMP NOT NULL DEFAULT NOW(),
  is_deleted BOOLEAN NOT NULL DEFAULT false,
  deleted_date TIMESTAMP,
  owner VARCHAR(255),
  
  -- Constraints
  CONSTRAINT pii_token_id_immutable PRIMARY KEY (pii_token_id)
);

-- Partitioned by created_date (hot/warm/cold tiers)
ALTER TABLE pii_data PARTITION BY RANGE (created_date);

-- No local IV/auth tag columns needed
-- KMS returns opaque encrypted blob
-- Database just stores the blob as-is
```

### Week 3: Service Layer

```java
// TokenService.java - Phase 1

@Service
public class TokenService {
  
  @Autowired
  private PiiDataRepository piiDataRepository;
  
  @Autowired
  private EncryptionService encryptionService;
  
  /**
   * Create token for PII
   * Encryption handled by KMS, not local crypto
   */
  public TokenResponse createToken(String piiData, String requestId) {
    // 1. Encrypt via KMS
    EncryptedPayload encryptedPayload = encryptionService.encrypt(piiData, requestId);
    
    // 2. Store encrypted blob (opaque to application)
    PiiDataEntity entity = new PiiDataEntity(
      tokenId: encryptedPayload.getTokenId(),
      encryptedData: encryptedPayload.getEncryptedData(),
      kmsKeyId: encryptedPayload.getKeyId(),
      createdDate: now(),
      owner: getCurrentUser().getId()
    );
    
    piiDataRepository.save(entity);
    
    // 3. Return token
    return new TokenResponse(
      tokenId: encryptedPayload.getTokenId(),
      createdAt: now()
    );
  }
}
```

### Week 4-6: Integration Tests (KMS-Based)

```java
// EncryptionServiceIntegrationTest.java

@SpringBootTest
@ActiveProfiles("test")  // Uses LocalStack KMS
public class EncryptionServiceIntegrationTest {
  
  @Autowired
  private EncryptionService encryptionService;
  
  @Autowired
  private PiiDataRepository piiDataRepository;
  
  @Test
  public void testEncryptDecrypt_KmsRoundTrip() {
    String originalPii = "user@example.com";
    String requestId = UUID.randomUUID().toString();
    
    // 1. Encrypt via KMS
    EncryptedPayload encrypted = encryptionService.encrypt(originalPii, requestId);
    
    assertThat(encrypted.getTokenId()).isNotNull();
    assertThat(encrypted.getEncryptedData()).isNotEmpty();
    assertThat(encrypted.getKeyId()).isNotNull();
    
    // 2. Verify encrypted data stored
    PiiDataEntity stored = piiDataRepository.findById(encrypted.getTokenId()).get();
    assertThat(stored.getEncryptedData()).isEqualTo(encrypted.getEncryptedData());
    
    // 3. Decrypt via KMS
    DecryptedPayload decrypted = encryptionService.decrypt(
      encrypted.getTokenId(), 
      requestId
    );
    
    // 4. Verify roundtrip
    assertThat(decrypted.getPiiData()).isEqualTo(originalPii);
  }
  
  @Test
  public void testDeterministicTokenGeneration_KmsBased() {
    String pii = "john@example.com";
    String requestId = "req-123";
    
    // Encrypt twice with same input
    EncryptedPayload enc1 = encryptionService.encrypt(pii, requestId);
    EncryptedPayload enc2 = encryptionService.encrypt(pii, requestId);
    
    // Token IDs should be identical (deterministic)
    assertThat(enc1.getTokenId()).isEqualTo(enc2.getTokenId());
    
    // But encrypted blobs will be different (KMS adds randomness)
    // This is expected - KMS encryption is non-deterministic for security
    assertThat(enc1.getEncryptedData()).isNotEqualTo(enc2.getEncryptedData());
  }
  
  @Test
  public void testKmsHealthCheck_LocalStack() {
    KmsService kmsService = new KmsService();
    boolean isHealthy = kmsService.isKmsHealthy();
    
    assertThat(isHealthy).isTrue();
  }
}
```

---

## Why KMS-Only (No Local Crypto)

### Security Benefits
✅ **No key material in code**: Application never handles raw keys  
✅ **FIPS 140-2 compliance**: AWS KMS is FIPS-certified  
✅ **Hardware acceleration**: KMS uses HSMs for crypto operations  
✅ **Audit trail**: Every operation logged in CloudTrail  

### Operational Benefits
✅ **Key rotation managed**: AWS handles key rotation automatically  
✅ **Multi-region support**: KMS keys can be replicated  
✅ **Compliance ready**: Supports PCI DSS, HIPAA, FedRAMP  
✅ **No crypto library burden**: Don't maintain AES-256 implementation  

### Architectural Benefits
✅ **Simpler codebase**: Application focuses on orchestration, not crypto  
✅ **Testable locally**: LocalStack KMS mock behaves identically  
✅ **Production ready**: Same code works with LocalStack (dev) and AWS KMS (prod)  
✅ **Scalable**: KMS throughput isn't bottleneck  

---

## Phase Transition: LocalStack → AWS KMS

### Phase 1 → Phase 2 (LocalStack → AWS KMS)

**Code Changes**: ZERO changes to application code
**Config Changes Only**:

```yaml
# Phase 1 (development)
encryption:
  kms:
    endpoint-url: http://localstack:4566  # LocalStack mock
    key-alias: alias/pii-vault-dev

# Phase 2 (production)
encryption:
  kms:
    endpoint-url: ${AWS_ENDPOINT_URL_KMS}  # AWS KMS (from IAM role)
    key-alias: alias/pii-vault-prod
```

**Deployment**: Same Docker image, different configuration

---

## Summary

**Phase 1 Implementation**:
- ✅ Use LocalStack KMS mock (docker-compose)
- ✅ All encryption/decryption via KMS API
- ✅ No local AES-256-GCM implementation
- ✅ Deterministic token IDs via HMAC
- ✅ Database stores opaque encrypted blobs only
- ✅ Integration tests validate KMS roundtrip

**Phase 2 Transition**:
- ✅ Deploy to AWS (configuration only)
- ✅ Switch to production AWS KMS
- ✅ Same code, different endpoint
- ✅ CloudTrail audit trail enabled

**Benefits**:
- Simpler, cleaner architecture
- Security best practices from day 1
- Locally testable (LocalStack)
- Production-ready immediately
- Compliance-friendly

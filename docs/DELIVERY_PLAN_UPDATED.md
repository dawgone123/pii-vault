# PII Vault - Comprehensive Feature Version Delivery Plan (Updated)

**Document Date**: July 2026  
**Plan Duration**: Q3 2026 - Q1 2027 (39 weeks)  
**Target Audience**: Engineering Team, Product Leadership, Stakeholders

---

## Executive Summary

PII Vault is a secure, enterprise-grade Spring Boot application for encryption/decryption of Personally Identifiable Information (PII) with double encryption architecture, high-throughput database optimization, and event streaming for data lake replication.

**Current Status**: Phase 1 MVP is approximately 40% complete with foundational Spring Boot 4.0.0 infrastructure and basic AES-256-GCM encryption working. Phase 2 and Phase 3 deliverables remain in the backlog.

**Key Changes in This Update**:
- **Phase 1**: Add database partitioning for high-throughput, dockerized local dev with VictoriaMetrics observability
- **Phase 2**: Add event definitions, Kafka CDC integration for data lake replication, performance testing
- **Phase 3**: Add block list capability, AWS Terraform infrastructure-as-code, performance benchmarking
- **Certification**: Move SOC 2 & PCI DSS compliance to deployment guidelines (clients implement)

---

## Table of Contents

1. [Phase 1 (Updated) - MVP + Local Dev Observability](#phase-1)
2. [Phase 2 (Updated) - Events, Kafka/CDC, Performance Testing](#phase-2)
3. [Phase 3 (Updated) - Block List, Terraform IaC, Benchmarking](#phase-3)
4. [Deployment Guidelines - SOC 2 & PCI DSS for Clients](#deployment-guide)
5. [Release Timeline & Milestones](#timeline)
6. [Resource Requirements](#resources)

---

## <a name="phase-1"></a>Phase 1 (MVP) - Q3 2026: Updated Roadmap (7 weeks)

### New Phase 1 Additions

**Database Partitioning for High-Throughput** (Week 2-3)
- Table partitioning by soft-delete date and created_date
- Partition pruning for query optimization
- Archive strategy for old partitions

**Dockerized Local Development with VictoriaMetrics** (Week 5-6)
- Docker Compose setup with application, PostgreSQL, Redis, VictoriaMetrics
- Observability dashboards for local development
- Detailed test plans and load testing framework

### Updated Phase 1 Timeline (7 weeks)

**Week 1: Foundation Hardening - KMS-Only Approach**
- Implement KMS-based encryption (no local AES-256-GCM)
  - LocalStack KMS mock for Phase 1 (docker-compose)
  - AWS KMS for Phase 2/3 (production)
  - Same code, different configuration
- Implement deterministic token generation (HMAC-based token_id)
- Add UUID v7 library integration
- Create AuditLog JPA entity with KMS key tracking

**Week 2-3: Database Schema & Partitioning (NEW)**
- Create `pii_data` table with partitioning strategy:
  ```sql
  -- Partition by soft-delete date (hot/warm/cold tiers)
  CREATE TABLE pii_data (
    pii_token_id UUID NOT NULL,
    encrypted_data TEXT NOT NULL,
    created_date TIMESTAMP NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_date TIMESTAMP,
    owner VARCHAR(255),
    kms_key_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (pii_token_id, created_date)  -- Composite key for partitioning
  ) PARTITION BY RANGE (created_date);
  
  -- Hot tier (0-30 days): Fast access, frequent queries
  CREATE TABLE pii_data_hot PARTITION OF pii_data
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
  
  -- Warm tier (30 days - 6 months): Occasional access
  CREATE TABLE pii_data_warm PARTITION OF pii_data
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
  
  -- Cold tier (6+ months): Archive, rare access
  CREATE TABLE pii_data_cold PARTITION OF pii_data
    FOR VALUES FROM ('2025-01-01') TO ('2026-06-01');
  ```

- Create `audit_logs` table with monthly partitioning:
  ```sql
  CREATE TABLE audit_logs (
    event_id BIGSERIAL NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    operation_type VARCHAR(50),
    pii_token_id UUID,
    owner VARCHAR(255),
    status VARCHAR(50),
    metadata JSONB,
    PRIMARY KEY (event_id, timestamp)
  ) PARTITION BY RANGE (timestamp);
  
  -- Monthly partitions (auto-created as new months arrive)
  CREATE TABLE audit_logs_2026_07 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
  ```

- Partition pruning queries for optimal performance
- Archive old partitions to cold storage (S3)
- Add AuditLog repository, KmsKeyMetadata entity

**Week 3: Service Layer & Soft-Delete**
- Implement AuditService, TokenService, SoftDeleteService
- Add immutability enforcement
- Implement soft-delete with partition-aware queries

**Week 4: Batch Operations & Correlation**
- Implement batch encrypt/decrypt with UUID v7
- Wire correlation IDs through request flow
- Update controllers with batch endpoints

**Week 5: Spring Security Configuration**
- Configure authentication providers
- Implement authorization interceptors
- Secure admin endpoints

**Week 5-6: Dockerized Local Development (NEW)**
- Create `docker-compose.yml`:
  ```yaml
  version: '3.9'
  services:
    pii-vault:
      build: .
      ports:
        - "8080:8080"
      environment:
        - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pii_vault
        - SPRING_DATASOURCE_USERNAME=postgres
        - SPRING_DATASOURCE_PASSWORD=postgres
        - REDIS_HOST=redis
        - REDIS_PORT=6379
        - METRICS_EXPORT_VICTORIAMETRICS_ENABLED=true
        - METRICS_EXPORT_VICTORIAMETRICS_STEP=1m
        - METRICS_EXPORT_VICTORIAMETRICS_URL=http://victoriametrics:8428
      depends_on:
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
      volumes:
        - postgres_data:/var/lib/postgresql/data
        - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql
      networks:
        - pii-vault-network

    redis:
      image: redis:7-alpine
      ports:
        - "6379:6379"
      volumes:
        - redis_data:/data
      networks:
        - pii-vault-network

    victoriametrics:
      image: victoriametrics/victoria-metrics:latest
      ports:
        - "8428:8428"
      volumes:
        - victoriametrics_data:/victoria-metrics-data
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
        - grafana_storage:/var/lib/grafana
      depends_on:
        - victoriametrics
      networks:
        - pii-vault-network

  volumes:
    postgres_data:
    redis_data:
    victoriametrics_data:
    grafana_storage:

  networks:
    pii-vault-network:
      driver: bridge
  ```

- Configure VictoriaMetrics integration in Spring Boot:
  ```java
  // In pom.xml
  <dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
  </dependency>
  
  // In application.yml
  management:
    metrics:
      export:
        prometheus:
          enabled: true
    endpoints:
      web:
        exposure:
          include: health,metrics,prometheus
  
  // Push metrics to VictoriaMetrics
  @Configuration
  public class MetricsConfig {
    @Bean
    public MeterBinder victoriametricsConfig() {
      return (registry) -> {
        // Configure Prometheus scrape via VictoriaMetrics
        registry.config().meterFilter(
          MeterFilter.maximumAllowableTags("http.server.requests", "uri", 100, MeterFilter.deny())
        );
      };
    }
  }
  ```

- Create Grafana dashboards for local dev:
  ```json
  {
    "dashboard": {
      "title": "PII Vault - Local Development",
      "panels": [
        {
          "title": "Encryption Throughput",
          "targets": [{"expr": "rate(pii_encrypt_operations_total[1m])"}]
        },
        {
          "title": "Encryption Latency (p95)",
          "targets": [{"expr": "histogram_quantile(0.95, pii_encrypt_latency_ms)"}]
        },
        {
          "title": "Database Connections",
          "targets": [{"expr": "pg_stat_activity_count"}]
        },
        {
          "title": "Cache Hit Ratio",
          "targets": [{"expr": "pii_cache_hit_ratio"}]
        }
      ]
    }
  }
  ```

- Load testing framework:
  ```java
  @SpringBootTest
  public class LoadTestSuite {
    
    @Test
    public void testEncryptionThroughput_1000OpsPerSecond() {
      Flux<PiiDataRequest> requests = Flux.range(0, 1000)
        .map(i -> new PiiDataRequest("test-email-" + i + "@example.com"));
      
      StepVerifier.create(
        requests.flatMap(req -> 
          piiVaultService.encryptAsync(req.getPiiData())
        )
      )
      .thenConsumeWhile(response -> 
        response.getLatencyMs() < 50  // Verify p95 < 50ms
      )
      .verifyComplete();
    }
    
    @Test
    public void testBatchOperations_10KBatch() {
      List<PiiDataRequest> batch = IntStream.range(0, 10000)
        .mapToObj(i -> new PiiDataRequest("test-" + i + "@example.com"))
        .collect(toList());
      
      long startTime = System.currentTimeMillis();
      BatchEncryptResponse response = piiVaultService.batchEncrypt(batch);
      long elapsed = System.currentTimeMillis() - startTime;
      
      assertThat(response.getSuccessCount()).isEqualTo(10000);
      assertThat(elapsed).isLessThan(1000);  // < 1 second for 10K
    }
  }
  ```

**Week 6: Testing, Documentation & Observability (NEW)**
- Comprehensive test plans (unit, integration, load tests)
- VictoriaMetrics observability dashboards
- Docker Compose documentation
- Local development guide
- Performance baseline establishment
- End-to-end testing documentation

### Phase 1 Success Criteria (Updated)

```
MUST HAVE:
✓ KMS-only encryption (LocalStack KMS mock in docker-compose)
✓ No local AES-256-GCM implementation (all crypto via KMS)
✓ Deterministic token_id generation (HMAC-based)
✓ Batch operations with UUID v7
✓ Immutability enforced
✓ Audit logs captured for all KMS operations
✓ Soft-delete with partitioning
✓ Database partitioning strategy (hot/warm/cold tiers)
✓ PostgreSQL partition pruning working
✓ Dockerized local development with LocalStack KMS
✓ VictoriaMetrics metrics collection
✓ Grafana dashboards for observability
✓ Load testing framework in place
✓ Spring Security configured
✓ KMS integration tests (LocalStack roundtrip)
✓ Test coverage > 70%

PERFORMANCE TARGETS:
✓ Encryption latency p95 < 50ms (KMS roundtrip)
✓ Decryption latency p95 < 50ms (KMS roundtrip)
✓ Batch throughput: 1000+ ops/sec
✓ Single instance: 5K+ ops/sec at p95
✓ LocalStack KMS latency consistent with AWS

KMS-SPECIFIC:
✓ LocalStack KMS mock working in docker-compose
✓ AWS KMS client properly configured
✓ CloudTrail-ready (for Phase 2)
✓ Key alias management in place
✓ Encryption context metadata tracked
✓ Zero local key material in code

OBSERVABILITY:
✓ Real-time metrics via VictoriaMetrics
✓ Local Grafana dashboards
✓ KMS operation latency tracking
✓ Database query performance tracking
```

---

## <a name="phase-2"></a>Phase 2 (ENHANCED FEATURES) - Q4 2026: Updated Roadmap (14 weeks)

### New Phase 2 Additions

**Event Definitions & Specifications**
- Token lifecycle events (CREATED, ACCESSED, DELETED, EXPIRED, INVALIDATED)
- Event schema with metadata, correlation IDs
- Event versioning for backward compatibility

**Kafka & CDC Integration for Data Lake Replication**
- Change Data Capture from PostgreSQL → Kafka
- Event serialization (Avro/Protocol Buffers)
- Data lake consumer integration
- End-to-end tracing through event pipeline

**Performance Testing & Benchmarking**
- Load testing at 10K ops/sec sustained
- Latency profiling (CPU, memory, database, KMS)
- Cache efficiency analysis
- Multi-instance horizontal scaling tests

### Updated Phase 2 Timeline (14 weeks)

#### Weeks 1-4: AWS KMS Integration (CRITICAL PATH)

[Same as original plan - see main DELIVERY_PLAN.md]

#### Week 2-3: Event Definitions & Specifications (PARALLEL)

**Event Schema Design**:
```java
@Entity
public class TokenEvent {
  @Id
  UUID eventId;
  
  UUID tokenId;                    // Token affected
  TokenEventType type;             // CREATED, ACCESSED, DELETED, EXPIRED, INVALIDATED
  
  LocalDateTime eventTimestamp;    // When event occurred
  LocalDateTime recordedAt;        // When logged (may differ from event time)
  
  String triggeredBy;              // User/service that triggered
  UUID requestId;                  // Correlation ID
  
  EventStatus status;              // SUCCESS, FAILURE, PARTIAL
  
  @Convert(converter = JsonConverter.class)
  Map<String, Object> metadata;    // Event-specific data
  
  Integer eventVersion;            // For schema evolution
  String source;                   // pii-vault-service
  
  // Token state snapshot at time of event
  String tokenState;               // ACTIVE, EXPIRED, DELETED, INVALIDATED
  LocalDateTime tokenExpiresAt;
}

// Event Type Specifications
enum TokenEventType {
  
  // Creation
  TOKEN_CREATED("1.0", new String[]{
    "piiType",           // Type of PII encrypted
    "keyId",            // KMS key used
    "encryptionLatencyMs",
    "batchSize"         // If part of batch operation
  }),
  
  // Access
  TOKEN_ACCESSED("1.0", new String[]{
    "operation",        // DECRYPT
    "accessLatencyMs",
    "cacheHit",         // true if served from cache
    "userId"            // Who accessed (audit)
  }),
  
  // Lifecycle
  TOKEN_EXPIRED("1.0", new String[]{
    "expirationReason",
    "ttlSeconds"
  }),
  
  TOKEN_DELETED("1.0", new String[]{
    "deletionReason",
    "requestedBy",
    "softDelete"        // true (not hard-deleted)
  }),
  
  TOKEN_INVALIDATED("1.0", new String[]{
    "invalidationReason",
    "keyRotationId"     // If due to key rotation
  }),
  
  // Admin operations
  TOKEN_RECOVERED("1.0", new String[]{
    "recoveredBy",
    "recoveryReason"
  });
  
  String schemaVersion;
  String[] requiredFields;
}

// Example Event Metadata
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "type": "TOKEN_CREATED",
  "eventTimestamp": "2026-10-15T08:30:00Z",
  "triggeredBy": "upstream-service-1",
  "requestId": "550e8400-e29b-41d4-a716-446655440002",
  "status": "SUCCESS",
  "metadata": {
    "piiType": "email",
    "keyId": "pii-vault-2026-10-15",
    "encryptionLatencyMs": 8,
    "batchSize": null
  },
  "eventVersion": 1,
  "source": "pii-vault-service"
}
```

**Event Publishing Service**:
```java
@Service
public class TokenEventPublisher {
  
  @Autowired
  private KafkaTemplate<String, TokenEvent> kafkaTemplate;
  
  @Autowired
  private EventRepository eventRepository;
  
  // Publish event to Kafka topic and store in database
  public void publishEvent(TokenEvent event) {
    // 1. Save to database (immutable event log)
    event.setRecordedAt(now());
    eventRepository.save(event);
    
    // 2. Publish to Kafka topic
    kafkaTemplate.send("pii-vault-token-events", event.getTokenId().toString(), event);
    
    // 3. Publish to specific event type topic
    kafkaTemplate.send("pii-vault-" + event.getType().getName().toLowerCase(), 
      event.getTokenId().toString(), event);
  }
  
  public void publishTokenCreated(UUID tokenId, String piiType, String keyId, long latencyMs) {
    publishEvent(new TokenEvent(
      eventId: UUID.randomUUID(),
      tokenId: tokenId,
      type: TOKEN_CREATED,
      eventTimestamp: now(),
      triggeredBy: getCurrentUser().getId(),
      requestId: RequestContextHolder.getRequestId(),
      status: SUCCESS,
      metadata: Map.of(
        "piiType", piiType,
        "keyId", keyId,
        "encryptionLatencyMs", latencyMs
      ),
      eventVersion: 1
    ));
  }
  
  public void publishTokenAccessed(UUID tokenId, boolean cacheHit, long latencyMs) {
    publishEvent(new TokenEvent(
      eventId: UUID.randomUUID(),
      tokenId: tokenId,
      type: TOKEN_ACCESSED,
      eventTimestamp: now(),
      triggeredBy: getCurrentUser().getId(),
      requestId: RequestContextHolder.getRequestId(),
      status: SUCCESS,
      metadata: Map.of(
        "operation", "DECRYPT",
        "accessLatencyMs", latencyMs,
        "cacheHit", cacheHit
      )
    ));
  }
}
```

**Kafka Configuration**:
```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Durability
      retries: 3
      properties:
        linger.ms: 10  # Batch messages for throughput
    consumer:
      bootstrap-servers: kafka:9092
      group-id: pii-vault-consumer
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

# Kafka Topics
kafka:
  topics:
    - name: pii-vault-token-events
      partitions: 10  # Parallel consumers
      replication-factor: 3  # High availability
      retention-ms: 7776000000  # 90 days
    
    - name: pii-vault-token-created
      partitions: 10
    
    - name: pii-vault-token-accessed
      partitions: 20  # Higher traffic
    
    - name: pii-vault-token-deleted
      partitions: 5
    
    - name: pii-vault-audit-events
      partitions: 5
      retention-ms: 157680000000  # 5 years (compliance)
```

#### Week 4-5: CDC Integration (Change Data Capture)

**PostgreSQL CDC with Debezium**:
```yaml
# Debezium connector configuration
{
  "name": "pii-vault-cdc-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": 5432,
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname": "pii_vault",
    "database.server.name": "pii-vault-db",
    "schema.include.list": "public",
    "table.include.list": "public.pii_data,public.audit_logs",
    
    # Enable logical replication slot
    "plugin.name": "pgoutput",
    "publication.name": "pii_vault_publication",
    
    # Kafka output
    "kafka.bootstrap.servers": "kafka:9092",
    "topics.prefix": "pii-vault-cdc",
    
    # Capture mode (changes only)
    "capture.mode": "logical",
    "snapshot.mode": "initial",
    
    # Output format
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": false,
    "value.converter.schemas.enable": false
  }
}
```

**Data Lake Consumer (Kafka → Data Lake)**:
```java
@Service
public class DataLakeConsumer {
  
  // Listen to CDC events and replicate to data lake
  @KafkaListener(topics = "pii-vault-cdc-pii_data", groupId = "data-lake-consumer")
  public void consumePiiDataChanges(ConsumerRecord<String, String> record) {
    // Parse CDC event
    CdcEvent cdcEvent = JsonUtil.parse(record.value(), CdcEvent.class);
    
    // Example CDC event structure:
    // {
    //   "before": null,  // null for INSERT
    //   "after": {
    //     "pii_token_id": "550e8400-e29b-41d4-a716-446655440001",
    //     "encrypted_data": "...",
    //     "created_date": "2026-10-15T08:30:00Z",
    //     "is_deleted": false
    //   },
    //   "op": "c",  // c=CREATE, u=UPDATE, d=DELETE
    //   "ts_ms": 1697353800000,
    //   "transaction": null
    // }
    
    switch (cdcEvent.getOp()) {
      case "c":  // INSERT - new token created
        replicateToDataLake(cdcEvent.getAfter());
        publishTokenCreatedEvent(cdcEvent.getAfter());
        break;
        
      case "u":  // UPDATE - token modified (soft-delete)
        if (isDeleted(cdcEvent.getAfter())) {
          publishTokenDeletedEvent(cdcEvent.getAfter());
        }
        replicateToDataLake(cdcEvent.getAfter());
        break;
        
      case "d":  // DELETE - hard delete (archive only)
        archiveFromDataLake(cdcEvent.getBefore());
        break;
    }
  }
  
  private void replicateToDataLake(Map<String, Object> piiData) {
    // Write to S3 Data Lake in Parquet format
    // Organize by: s3://pii-vault-dl/year=2026/month=10/day=15/hour=08/pii-data-*.parquet
    
    dataLakeService.writeParquet(
      bucket: "pii-vault-data-lake",
      path: generateDLPath(piiData.get("created_date")),
      data: piiData,
      format: "PARQUET"  // Efficient columnar format
    );
  }
}
```

**Data Lake Consumer Topology**:
```
PostgreSQL  →  [Logical Replication]  →  Debezium  →  Kafka  →  Consumer  →  S3 Data Lake
  (CDC)           (publication)                          Topic                  (Parquet)
```

#### Week 6-7: Token Lifecycle + Audit (continue from original plan)

#### Week 8: Event Delivery Guarantees

**Kafka Partition Strategy for Ordering**:
```java
// Ensure events for same token go to same partition (ordering)
kafkaTemplate.send(
  new ProducerRecord<>(
    "pii-vault-token-events",
    0,  // Fixed partition
    tokenId.toString(),  // Key (ensures same token → same partition)
    event  // Value
  )
);

// Partition count = 10 (configurable, balance parallelism vs. ordering)
// Each data lake consumer thread processes 1-2 partitions sequentially
```

**Event Delivery Guarantee Options**:
```yaml
spring:
  kafka:
    producer:
      acks: all              # Wait for all replicas to ack
      retries: -1            # Infinite retries
      delivery-timeout-ms: 300000  # 5 minutes
      properties:
        enable.idempotence: true  # Prevent duplicates
        transactional.id: pii-vault-producer
        max.in.flight.requests.per.connection: 5
        
# Result: At-Least-Once delivery with idempotent producers
# (duplicate suppression for exactly-once semantics)
```

#### Week 9-10: Performance Testing & Benchmarking (NEW)

**Performance Testing Framework**:
```java
@SpringBootTest
public class PerformanceBenchmarkSuite {
  
  @Autowired
  private PiiVaultService piiVaultService;
  
  @Autowired
  private MeterRegistry meterRegistry;
  
  // Test 1: Sustained Load at 10K ops/sec
  @Test
  public void benchmarkSustainedLoad_10KOpsPerSecond() throws Exception {
    int targetOpsPerSecond = 10000;
    int durationSeconds = 60;
    int expectedTotalOps = targetOpsPerSecond * durationSeconds;
    
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    
    // Simulate 10K requests/sec for 60 seconds
    CountDownLatch completionLatch = new CountDownLatch(expectedTotalOps);
    
    for (int i = 0; i < expectedTotalOps; i++) {
      executeAsync(() -> {
        long startTime = System.nanoTime();
        try {
          PiiDataResponse response = piiVaultService.encrypt(
            new PiiDataRequest("test-" + i + "@example.com")
          );
          long latencyMs = (System.nanoTime() - startTime) / 1_000_000;
          latencies.add(latencyMs);
          successCount.incrementAndGet();
        } catch (Exception e) {
          failureCount.incrementAndGet();
        } finally {
          completionLatch.countDown();
        }
      });
    }
    
    boolean completed = completionLatch.await(durationSeconds + 30, TimeUnit.SECONDS);
    assertTrue(completed, "Benchmark did not complete within expected time");
    
    // Analyze results
    PerformanceReport report = analyzeLatencies(latencies, successCount.get(), failureCount.get());
    
    assertThat(report.getP50LatencyMs()).isLessThan(20);
    assertThat(report.getP95LatencyMs()).isLessThan(50);
    assertThat(report.getP99LatencyMs()).isLessThan(100);
    assertThat(report.getThroughputOpsPerSec()).isGreaterThanOrEqualTo(targetOpsPerSecond);
    assertThat(report.getErrorRate()).isLessThan(0.01);  // < 1%
  }
  
  // Test 2: Cache Hit Efficiency
  @Test
  public void benchmarkCacheEfficiency() {
    String tokenId = "test-token-123";
    
    // Warm up cache
    piiVaultService.decrypt(tokenId);
    
    // Measure cache hits
    List<Long> cacheHitLatencies = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      long start = System.nanoTime();
      piiVaultService.decrypt(tokenId);  // Should hit cache
      long latencyMs = (System.nanoTime() - start) / 1_000_000;
      cacheHitLatencies.add(latencyMs);
    }
    
    double avgCacheHitLatency = cacheHitLatencies.stream()
      .mapToLong(Long::longValue)
      .average()
      .orElse(0);
    
    // Cache hits should be 100x faster than DB queries
    assertTrue(avgCacheHitLatency < 1, "Cache hit latency too high: " + avgCacheHitLatency);
  }
  
  // Test 3: Multi-Instance Horizontal Scaling
  @Test
  public void benchmarkHorizontalScaling() {
    // Test with 1 instance
    int throughput1 = measureThroughput(1);  // ops/sec
    
    // Test with 3 instances
    int throughput3 = measureThroughput(3);
    
    // Test with 5 instances
    int throughput5 = measureThroughput(5);
    
    // Verify near-linear scaling (target 80%+ efficiency)
    double scalingEfficiency3 = (throughput3 / (double) throughput1) / 3;
    double scalingEfficiency5 = (throughput5 / (double) throughput1) / 5;
    
    assertThat(scalingEfficiency3).isGreaterThanOrEqualTo(0.8);  // 80% scaling efficiency
    assertThat(scalingEfficiency5).isGreaterThanOrEqualTo(0.8);
  }
  
  // Test 4: Database Connection Pool Performance
  @Test
  public void benchmarkDatabaseConnections() {
    HikariDataSource dataSource = (HikariDataSource) hikariConfig.getDataSource();
    
    // Verify connection pool is never exhausted
    for (int i = 0; i < 10000; i++) {
      try (Connection conn = dataSource.getConnection()) {
        // Connection acquired successfully
        assertTrue(dataSource.getHikariPoolMXBean().getActiveConnections() <= 
                  dataSource.getHikariPoolMXBean().getMaximumPoolSize());
      }
    }
  }
  
  // Test 5: KMS API Call Performance
  @Test
  public void benchmarkKmsLatency() {
    List<Long> kmsLatencies = new ArrayList<>();
    
    for (int i = 0; i < 100; i++) {
      long start = System.nanoTime();
      kmsService.encryptApplicationKey("test-key-" + i);
      long latencyMs = (System.nanoTime() - start) / 1_000_000;
      kmsLatencies.add(latencyMs);
    }
    
    DoubleSummaryStatistics stats = kmsLatencies.stream()
      .mapToDouble(Long::doubleValue)
      .summaryStatistics();
    
    assertThat(stats.getAverage()).isLessThan(20);  // Avg < 20ms
    
    List<Long> sorted = kmsLatencies.stream().sorted().collect(toList());
    assertThat(sorted.get((int) (sorted.size() * 0.95))).isLessThan(50);  // p95 < 50ms
  }
  
  private PerformanceReport analyzeLatencies(List<Long> latencies, int success, int failure) {
    List<Long> sorted = latencies.stream().sorted().collect(toList());
    
    return new PerformanceReport(
      p50LatencyMs: sorted.get((int) (sorted.size() * 0.50)),
      p95LatencyMs: sorted.get((int) (sorted.size() * 0.95)),
      p99LatencyMs: sorted.get((int) (sorted.size() * 0.99)),
      avgLatencyMs: latencies.stream().mapToLong(Long::longValue).average().orElse(0),
      minLatencyMs: latencies.stream().mapToLong(Long::longValue).min().orElse(0),
      maxLatencyMs: latencies.stream().mapToLong(Long::longValue).max().orElse(0),
      successCount: success,
      failureCount: failure,
      errorRate: failure / (double) (success + failure),
      throughputOpsPerSec: success / 60  // Per second
    );
  }
}
```

**Performance Test Execution Schedule**:
- Weekly during development (identify bottlenecks early)
- Before each release (ensure SLAs met)
- After infrastructure changes (KMS, DB updates)
- Load testing in production-like environment

#### Weeks 11-13: Monitoring & Rate Limiting (continue from original plan)

### Phase 2 Success Criteria (Updated)

```
✓ Event system operational
  - 100+ events/sec published to Kafka
  - At-least-once delivery guarantee
  - Event ordering per token (partitioning)

✓ CDC Integration working
  - PostgreSQL changes streamed to Kafka
  - Data lake receiving events
  - Consumers lag < 1 second

✓ Performance Targets Achieved
  - 10K ops/sec sustained throughput
  - Encryption latency p95 < 50ms
  - Decryption latency p95 < 50ms
  - Cache hit latency < 1ms
  - KMS call latency p95 < 50ms
  - Error rate < 0.1%

✓ Horizontal Scaling Verified
  - 3-5 instances scale to 80%+ efficiency
  - Database connection pooling adequate
  - Kafka partitions balanced

✓ All Phase 2 features operational
  - KMS double encryption
  - Token expiration
  - JWT authentication
  - Rate limiting
  - Monitoring & alerting
```

---

## <a name="phase-3"></a>Phase 3 (HARDENING & SCALING) - Q1 2027: Updated Roadmap (13 weeks)

### New Phase 3 Additions

**Block List Capability**
- Token block list for fraud prevention
- Bulk block operations
- Block list query optimization

**AWS Terraform Infrastructure-as-Code**
- Complete IaC for all AWS resources
- Multi-region configuration
- Disaster recovery automation

**Performance Benchmarking at Scale**
- 100K+ ops/sec peak load testing
- Multi-region performance validation
- Customer scenario simulations

### Updated Phase 3 Timeline (13 weeks)

#### Week 1-4: Performance Optimization (EXPANDED)

[Performance optimization from original plan, plus:]

**Week 1-2: Add Block List Implementation**

```java
@Entity
public class TokenBlockList {
  @Id
  UUID id;
  
  UUID tokenId;
  
  BlocklistReason reason;          // FRAUD, USER_REQUESTED, SECURITY, COMPROMISE
  String description;
  
  @CreationTimestamp
  LocalDateTime blockedAt;
  
  String blockedBy;                // Admin or system
  
  @OneToMany
  List<BlockListAudit> auditTrail;
  
  LocalDateTime unblockDeadline;   // When auto-unblock occurs (if applicable)
  boolean permanent;               // true = never auto-unblock
}

enum BlocklistReason {
  FRAUD("Customer requested block due to fraud"),
  USER_REQUESTED("User requested token deletion"),
  SECURITY("Security incident requiring block"),
  COMPLIANCE("Compliance requirement"),
  COMPROMISE("Suspected token compromise");
  
  String description;
}

@Service
public class TokenBlockListService {
  
  // Single block operation
  public void blockToken(UUID tokenId, BlocklistReason reason, String description) {
    TokenBlockList blockEntry = new TokenBlockList(
      tokenId: tokenId,
      reason: reason,
      description: description,
      blockedBy: getCurrentUser().getId(),
      permanent: reason == FRAUD || reason == COMPROMISE
    );
    
    blockListRepository.save(blockEntry);
    
    // Invalidate any cached entries
    cacheService.evict("tokens", tokenId);
    
    auditService.log(TOKEN_BLOCKED, getCurrentUser().getId(), tokenId, reason);
  }
  
  // Bulk block operation (e.g., "block all tokens for this customer")
  public BulkBlockResult blockTokensInBulk(List<UUID> tokenIds, BlocklistReason reason) {
    List<TokenBlockList> entries = tokenIds.stream()
      .map(tokenId -> new TokenBlockList(
        tokenId: tokenId,
        reason: reason,
        blockedBy: getCurrentUser().getId()
      ))
      .collect(toList());
    
    blockListRepository.saveAll(entries);
    
    // Batch evict from cache
    cacheService.evictBatch("tokens", tokenIds);
    
    return new BulkBlockResult(
      successful: entries.size(),
      failed: 0,
      timestamp: now()
    );
  }
  
  // Check if token is blocked (optimized)
  public boolean isTokenBlocked(UUID tokenId) {
    // Use Bloom filter for O(1) lookup on large block lists
    return blockListBloomFilter.contains(tokenId) ||
           blockListRepository.existsByTokenId(tokenId);
  }
  
  // Query block list
  public List<TokenBlockList> getBlockedTokensFor(String customerId) {
    return blockListRepository.findByBlockedByAndCreatedAtAfter(
      customerId,
      now().minusMonths(6)
    );
  }
}

// Block List Bloom Filter for O(1) lookup
@Configuration
public class BlockListBloomFilterConfig {
  
  @Bean
  public BloomFilter<UUID> blockListBloomFilter() {
    // FalsePositiveRate = 1% (acceptable for fraud detection)
    return BloomFilter.create(
      Funnels.uuids(),
      estimatedElements: 10_000_000,  // 10M tokens
      fpp: 0.01
    );
  }
  
  @Scheduled(fixedRate = 60000)  // Every minute
  public void rebuildBlockListBloomFilter() {
    List<UUID> blockedTokenIds = blockListRepository.findAllBlockedTokenIds();
    BloomFilter<UUID> newFilter = BloomFilter.create(
      Funnels.uuids(),
      blockedTokenIds.size(),
      0.01
    );
    
    blockedTokenIds.forEach(newFilter::put);
    blockListBloomFilter = newFilter;
  }
}
```

**Block List Performance Optimization**:
```java
// Lookup is O(1) via Bloom filter
public PiiDataResponse decrypt(String tokenId) {
  UUID tokenUuid = UUID.fromString(tokenId);
  
  // Fast bloom filter check (O(1))
  if (blockListService.isTokenBlocked(tokenUuid)) {
    auditService.log(DECRYPT_BLOCKED_TOKEN, getCurrentUser().getId(), tokenUuid);
    throw new BlockedTokenException("Token is on block list");
  }
  
  // Proceed with normal decrypt
  return piiVaultService.decrypt(tokenUuid);
}
```

#### Week 2-3: AWS Terraform Infrastructure (NEW)

**Complete Terraform Configuration**:

```hcl
# vpc.tf - Network setup
resource "aws_vpc" "pii_vault" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "pii-vault-vpc"
  }
}

# Multi-region: replica VPC
resource "aws_vpc" "pii_vault_secondary" {
  provider             = aws.secondary
  cidr_block           = "10.1.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "pii-vault-vpc-secondary"
  }
}

# rds.tf - Database
resource "aws_rds_cluster" "pii_vault" {
  cluster_identifier      = "pii-vault-cluster"
  engine                  = "aurora-postgresql"
  engine_version          = "15.2"
  database_name           = "pii_vault"
  master_username         = "postgres"
  master_password         = var.db_password
  backup_retention_period = 35  # 5 weeks (PCI DSS)
  preferred_backup_window = "03:00-04:00"
  
  db_subnet_group_name              = aws_db_subnet_group.pii_vault.name
  db_cluster_parameter_group_name   = aws_rds_cluster_parameter_group.pii_vault.name
  vpc_security_group_ids            = [aws_security_group.rds.id]
  
  enabled_cloudwatch_logs_exports = ["postgresql"]
  
  storage_encrypted           = true
  kms_key_id                  = aws_kms_key.rds.arn
  
  # Enable automated minor version upgrades
  auto_minor_version_upgrade = true
  
  # Enable binary logging for CDC
  enable_http_endpoint = true
  
  tags = {
    Name = "pii-vault-db"
  }
}

# Add read replica in secondary region
resource "aws_rds_cluster_instance" "pii_vault_secondary" {
  provider           = aws.secondary
  cluster_identifier = aws_rds_cluster.pii_vault_secondary.id
  instance_class     = "db.r5.xlarge"
  engine              = aws_rds_cluster.pii_vault_secondary.engine
  engine_version      = aws_rds_cluster.pii_vault_secondary.engine_version
  
  performance_insights_enabled    = true
  monitoring_interval             = 60
  monitoring_role_arn            = aws_iam_role.rds_monitoring.arn
}

# kms.tf - Key Management
resource "aws_kms_key" "pii_vault" {
  description             = "PII Vault primary encryption key"
  deletion_window_in_days = 30
  enable_key_rotation     = true  # Automatic annual rotation
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM policies"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow PII Vault service"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.pii_vault_app.arn
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:GenerateDataKey",
          "kms:DescribeKey"
        ]
        Resource = "*"
      }
    ]
  })
  
  tags = {
    Name = "pii-vault-key"
  }
}

# Multi-region key replication
resource "aws_kms_replica_key" "pii_vault_secondary" {
  provider                = aws.secondary
  description             = "PII Vault replica encryption key (secondary region)"
  primary_key_id          = aws_kms_key.pii_vault.id
  deletion_window_in_days = 30
  
  tags = {
    Name = "pii-vault-key-secondary"
  }
}

# eks.tf - Kubernetes
resource "aws_eks_cluster" "pii_vault" {
  name            = "pii-vault-cluster"
  role_arn        = aws_iam_role.eks_cluster.arn
  version         = "1.28"
  
  vpc_config {
    subnet_ids              = aws_subnet.private[*].id
    security_groups         = [aws_security_group.eks.id]
    endpoint_private_access = true
    endpoint_public_access  = true
  }
  
  enabled_cluster_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]
  
  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster_policy,
    aws_iam_role_policy_attachment.eks_service_policy
  ]
  
  tags = {
    Name = "pii-vault-eks"
  }
}

resource "aws_eks_node_group" "pii_vault" {
  cluster_name    = aws_eks_cluster.pii_vault.name
  node_group_name = "pii-vault-nodes"
  node_role_arn   = aws_iam_role.eks_node.arn
  subnet_ids      = aws_subnet.private[*].id
  
  scaling_config {
    desired_size = 5
    max_size     = 20
    min_size     = 3
  }
  
  instance_types = ["t3.xlarge"]
  
  tags = {
    Name = "pii-vault-nodes"
  }
}

# elasticache.tf - Redis
resource "aws_elasticache_cluster" "pii_vault" {
  cluster_id           = "pii-vault-cache"
  engine               = "redis"
  node_type           = "cache.r5.xlarge"
  num_cache_nodes     = 3
  parameter_group_name = "default.redis7"
  port                = 6379
  
  engine_version       = "7.0"
  parameter_group_name = aws_elasticache_parameter_group.pii_vault.name
  
  security_group_ids   = [aws_security_group.elasticache.id]
  subnet_group_name    = aws_elasticache_subnet_group.pii_vault.name
  
  # Multi-AZ with automatic failover
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  # Encryption at rest
  at_rest_encryption_enabled = true
  kms_key_id                = aws_kms_key.pii_vault.arn
  
  # Encryption in transit
  transit_encryption_enabled = true
  auth_token               = var.redis_auth_token
  
  # Logging
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis.name
    destination_type = "cloudwatch-logs"
    log_format       = "json"
    log_type         = "slow-log"
  }
  
  # Backup
  snapshot_retention_limit = 35
  snapshot_window         = "03:00-05:00"
  
  tags = {
    Name = "pii-vault-cache"
  }
}

# msk.tf - Kafka (MSK)
resource "aws_msk_cluster" "pii_vault" {
  cluster_name           = "pii-vault-kafka"
  kafka_version          = "3.5.0"
  number_of_broker_nodes = 3
  
  broker_node_group_info {
    instance_type   = "kafka.m5.xlarge"
    security_groups = [aws_security_group.kafka.id]
    storage_info {
      ebs_storage_info {
        volume_size = 1000
      }
    }
  }
  
  encryption_info {
    encryption_at_rest {
      data_volume_kms_key_id = aws_kms_key.kafka.arn
    }
    encryption_in_transit {
      client_broker = "TLS"
    }
  }
  
  client_authentication {
    sasl {
      iam = true
    }
  }
  
  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.kafka.name
      }
    }
  }
  
  tags = {
    Name = "pii-vault-kafka"
  }
}

# s3.tf - Data Lake
resource "aws_s3_bucket" "data_lake" {
  bucket = "pii-vault-data-lake-${data.aws_caller_identity.current.account_id}"
  
  tags = {
    Name = "pii-vault-data-lake"
  }
}

resource "aws_s3_bucket_versioning" "data_lake" {
  bucket = aws_s3_bucket.data_lake.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "data_lake" {
  bucket = aws_s3_bucket.data_lake.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.pii_vault.arn
    }
  }
}

# Lifecycle policy: move to Glacier after 90 days
resource "aws_s3_bucket_lifecycle_configuration" "data_lake" {
  bucket = aws_s3_bucket.data_lake.id

  rule {
    id     = "archive-old-data"
    status = "Enabled"

    transition {
      days          = 90
      storage_class = "GLACIER"
    }
  }
}

# IAM roles
resource "aws_iam_role" "pii_vault_app" {
  name = "pii-vault-app-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "pii_vault_app" {
  name = "pii-vault-app-policy"
  role = aws_iam_role.pii_vault_app.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey",
          "kms:DescribeKey"
        ]
        Resource = aws_kms_key.pii_vault.arn
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.data_lake.arn}/*"
      },
      {
        Effect = "Allow"
        Action = [
          "kafka-cluster:*"
        ]
        Resource = aws_msk_cluster.pii_vault.arn
      }
    ]
  })
}

# Monitoring
resource "aws_cloudwatch_log_group" "pii_vault" {
  name              = "/aws/pii-vault/application"
  retention_in_days = 90
}

resource "aws_sns_topic" "pii_vault_alerts" {
  name = "pii-vault-alerts"
}

resource "aws_cloudwatch_metric_alarm" "encryption_latency_high" {
  alarm_name          = "pii-vault-encryption-latency-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "pii.encrypt.latency.p95"
  namespace           = "PIIVault"
  period              = 300
  statistic           = "Average"
  threshold           = 50
  alarm_description   = "Encryption latency p95 exceeds 50ms"
  alarm_actions       = [aws_sns_topic.pii_vault_alerts.arn]
}

# Terraform variables
variable "aws_region" {
  default = "us-east-1"
}

variable "aws_region_secondary" {
  default = "us-west-2"
}

variable "environment" {
  default = "production"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "redis_auth_token" {
  type      = string
  sensitive = true
}

# Outputs
output "eks_cluster_endpoint" {
  value = aws_eks_cluster.pii_vault.endpoint
}

output "rds_cluster_endpoint" {
  value = aws_rds_cluster.pii_vault.cluster_resource_id
}

output "kafka_bootstrap_servers" {
  value = aws_msk_cluster.pii_vault.bootstrap_brokers_tls
}

output "s3_data_lake_bucket" {
  value = aws_s3_bucket.data_lake.id
}
```

**Terraform Module Structure**:
```
terraform/
├── main.tf                 # Provider config, multi-region
├── vpc.tf                  # Network infrastructure
├── rds.tf                  # Database (primary + secondary)
├── kms.tf                  # Encryption keys
├── eks.tf                  # Kubernetes
├── elasticache.tf          # Redis cluster
├── msk.tf                  # Kafka/MSK
├── s3.tf                   # Data lake
├── iam.tf                  # IAM roles & policies
├── monitoring.tf           # CloudWatch, alarms
├── variables.tf            # Variable definitions
├── terraform.tfvars        # Environment-specific values
├── outputs.tf              # Outputs for other tools
└── modules/
    ├── networking/         # Reusable network module
    ├── database/           # Reusable database module
    └── kubernetes/         # Reusable K8s module
```

**Terraform Deployment**:
```bash
# Initialize
terraform init

# Plan multi-region deployment
terraform plan -out=tfplan

# Apply with approval
terraform apply tfplan

# Output important values
terraform output -json > infrastructure.json

# Destroy (if needed)
terraform destroy
```

#### Week 4-6: Performance Benchmarking at Scale (EXPANDED)

**Peak Load Testing (100K+ ops/sec)**:
```java
@SpringBootTest
@ActiveProfiles("load-test")
public class ScalePerformanceBenchmarkSuite {
  
  // Test: 100K ops/sec peak load
  @Test
  public void benchmarkPeakLoad_100KOpsPerSecond() {
    int targetOpsPerSecond = 100_000;
    int durationSeconds = 300;  // 5 minutes
    int expectedTotalOps = targetOpsPerSecond * durationSeconds;
    
    PerformanceReport report = loadTest(expectedTotalOps, durationSeconds);
    
    // Verify SLAs at peak
    assertThat(report.getP95LatencyMs()).isLessThan(100);
    assertThat(report.getP99LatencyMs()).isLessThan(200);
    assertThat(report.getErrorRate()).isLessThan(0.01);  // < 1%
    assertThat(report.getThroughputOpsPerSec()).isGreaterThanOrEqualTo(targetOpsPerSecond);
  }
  
  // Test: Multi-region performance consistency
  @Test
  public void benchmarkMultiRegionPerformance() {
    RegionPerformance primaryRegion = measureRegionPerformance("us-east-1");
    RegionPerformance secondaryRegion = measureRegionPerformance("us-west-2");
    
    // Secondary region should be within 10% of primary
    assertThat(Math.abs(primaryRegion.getLatencyMs() - secondaryRegion.getLatencyMs()))
      .isLessThan(primaryRegion.getLatencyMs() * 0.10);
  }
  
  // Test: Customer scenario simulation
  @Test
  public void benchmarkCustomerScenario_FinancialServices() {
    // Simulate typical financial services usage:
    // - 70% single encrypt/decrypt operations
    // - 20% batch operations (100-1000 items)
    // - 10% admin operations
    
    CustomerScenario scenario = new CustomerScenario(
      name: "Financial Services",
      singleOpsRatio: 0.70,
      batchOpsRatio: 0.20,
      adminOpsRatio: 0.10,
      targetOpsPerSec: 50_000,
      durationMinutes: 60
    );
    
    PerformanceReport report = simulateCustomerWorkload(scenario);
    
    assertThat(report.getP95LatencyMs()).isLessThan(50);
    assertThat(report.getDataLakeLagSeconds()).isLessThan(5);
  }
}
```

#### Week 7-13: Multi-Region & Disaster Recovery (continue from Phase 3 original plan)

### Phase 3 Success Criteria (Updated)

```
✓ Block List Capability
  - Sub-millisecond block check (Bloom filter)
  - Bulk block operations (< 1 second for 100K tokens)
  - 100% block list accuracy

✓ Terraform Infrastructure
  - All AWS resources defined as code
  - Multi-region configuration working
  - Reproducible deployments
  - Documentation complete

✓ Performance at Scale
  - 100K+ ops/sec peak load sustained
  - Encryption latency p95 < 100ms at peak
  - Multi-region latency parity (within 10%)
  - Error rate < 0.01% (< 1 error per 10K ops)

✓ Customer Scenarios
  - Financial services workload: 50K ops/sec
  - Healthcare workload: 30K ops/sec
  - E-commerce workload: 75K ops/sec

✓ Full production readiness
  - Terraform deployments repeatable
  - Multi-region failover < 10 minutes
  - Block list deployed and operational
  - Customer compliance guidelines published
```

---

## <a name="deployment-guide"></a>Deployment Guidelines - SOC 2 & PCI DSS for Clients

**Important Shift**: SOC 2 Type II and PCI DSS compliance certifications have been moved to **deployment guidelines for clients**. PII Vault provides the technology foundation and guidance; customer organizations implementing PII Vault are responsible for achieving their own certifications.

### What PII Vault Provides

✅ **Security Controls Foundation**:
- AES-256-GCM encryption
- AWS KMS key management
- Audit logging framework
- Access control via JWT/RBAC
- Rate limiting capabilities
- Multi-region high availability
- Disaster recovery capabilities
- Event streaming for compliance tracking

✅ **Compliance-Ready Features**:
- Immutable audit logs (for PCI DSS Requirement 10)
- Data retention policies with destruction schedules
- Change management framework
- Incident response capabilities
- Encryption in transit and at rest

✅ **Operational Support**:
- Docker containerization
- Kubernetes manifests
- Terraform IaC for AWS
- VictoriaMetrics observability
- Performance benchmarks
- Security hardening guidelines

### What Customers Must Do

**For SOC 2 Type II Certification** (Weeks 1-26 after deployment):

1. **Engage SOC 2 Auditor** (Week 1)
   - Select AICPA-credentialed auditor
   - Define scope (PII Vault + customer's infrastructure)
   - Establish 6-month observation period

2. **Implement Organizational Controls** (Weeks 2-13)
   - Access management (customer's identity provider)
   - Change management (customer's deployment process)
   - Incident response (customer's SOC)
   - Data retention policies (customer's business requirements)
   - Monitoring & alerting (customer's NOC)

3. **Evidence Collection** (Weeks 5-24)
   - PII Vault audit logs (provided by PII Vault)
   - Customer policy documentation
   - Change approval records
   - Incident response records
   - Access review documentation

4. **Audit Completion** (Weeks 25-26)
   - Auditor issues SOC 2 Type II report
   - Customer shares report with their customers

**For PCI DSS v4 Compliance** (Ongoing):

1. **Shared Responsibility Model**:
   ```
   PII Vault Provides:
   ├─ Requirement 3: Encryption (AES-256-GCM) ✓
   ├─ Requirement 4: Encryption in transit (TLS 1.3+) ✓
   ├─ Requirement 10: Logging & monitoring (audit logs) ✓
   └─ Requirement 12.1: Security policies (guidance) ✓
   
   Customer Must Implement:
   ├─ Requirement 1: Network security
   ├─ Requirement 2: Default passwords/security parameters
   ├─ Requirement 5: Anti-malware software
   ├─ Requirement 6: Security updates & vulnerability scanning
   ├─ Requirement 7-9: Access control, physical security
   ├─ Requirement 11: Testing & monitoring
   └─ Requirement 12: Security policies (full implementation)
   ```

2. **Customer Deployment Checklist**:
   ```
   Network Segmentation
   ├─ [ ] PII Vault isolated in private subnets
   ├─ [ ] Security groups restrict inbound access
   └─ [ ] VPN/private link for management access
   
   Access Control
   ├─ [ ] MFA enabled for all admin access
   ├─ [ ] IAM roles follow least privilege
   ├─ [ ] Service-to-service auth via IAM
   └─ [ ] Regular access reviews (quarterly)
   
   Encryption
   ├─ [ ] KMS keys created in customer's AWS account
   ├─ [ ] Key rotation enabled (annual minimum)
   ├─ [ ] TLS 1.3+ enforced for API
   └─ [ ] Data lake encryption at rest
   
   Monitoring
   ├─ [ ] CloudWatch logs aggregated & retained
   ├─ [ ] KMS CloudTrail logging enabled
   ├─ [ ] Database audit logging enabled
   ├─ [ ] Alerts configured for suspicious activity
   └─ [ ] Regular log review (monthly)
   
   Incident Response
   ├─ [ ] Incident response procedure documented
   ├─ [ ] On-call rotation established
   ├─ [ ] Incident communication plan defined
   ├─ [ ] Post-incident review process
   └─ [ ] Evidence retention policy
   
   Disaster Recovery
   ├─ [ ] Backup retention policy documented
   ├─ [ ] Quarterly DR testing completed
   ├─ [ ] RTO/RPO targets defined & met
   ├─ [ ] Runbooks for failover procedures
   └─ [ ] Multi-region configuration tested
   ```

### PII Vault Compliance Documentation for Customers

PII Vault provides these documents to customers:

**1. Security Architecture Guide** (SOC2_ROADMAP.md)
- Control design and implementation
- Evidence collection procedures
- Audit preparation guidance

**2. PCI DSS Implementation Guide**
- Shared responsibility mapping
- Encryption verification procedures
- Audit logging validation
- KMS key management best practices

**3. Deployment Hardening Guide**
- AWS Terraform best practices
- Kubernetes security configuration
- Network segmentation strategies
- Monitoring setup procedures

**4. Compliance Playbooks**
- PCI DSS auditor response template
- SOC 2 auditor interview preparation
- Incident notification procedures
- Regulatory requirement mapping

### Customer Certification Timeline

```
Month 1:   Deploy PII Vault
           ├─ Set up infrastructure (1-2 weeks)
           └─ Enable audit logging & monitoring (1-2 weeks)

Month 2-4: Prepare for SOC 2 audit
           ├─ Engage SOC 2 auditor (Month 2)
           ├─ Implement organizational controls (Months 2-3)
           └─ Observation period begins (Month 4)

Month 5-8: SOC 2 observation & evidence collection
           ├─ Monthly auditor reviews
           ├─ Quarterly on-site visits
           └─ Quarterly DR testing

Month 9-10: Audit completion
            ├─ Final auditor interviews
            ├─ Draft report preparation
            └─ Management review

Month 10-11: Report issuance
             ├─ Final SOC 2 Type II report
             └─ Customer begins sharing with prospects/partners

Ongoing:   PCI DSS compliance
           ├─ Annual penetration testing
           ├─ Quarterly vulnerability scanning
           ├─ Monthly log review
           └─ Annual SAQ completion
```

---

## <a name="timeline"></a>Release Timeline & Milestones

### Overall Updated Timeline

```
Phase 1 (Jul-Sep 2026):   7 weeks (was 6)
├─ Weeks 1-3: Crypto hardening + DB partitioning
├─ Weeks 4-5: Batch ops + Spring Security
├─ Weeks 5-6: Docker dev + VictoriaMetrics
└─ Deliverable: v1.0.0 with local observability

Phase 2 (Oct-Dec 2026):   14 weeks (was 13)
├─ Weeks 1-4: KMS integration
├─ Weeks 2-3: Event definitions
├─ Weeks 4-5: Kafka CDC integration
├─ Weeks 6-9: Token lifecycle + audit
├─ Weeks 8-9: JWT/OAuth2
├─ Weeks 7-8: Caching
├─ Weeks 9-11: Performance testing
├─ Weeks 10-11: Rate limiting
└─ Deliverable: v2.0.0 with events & CDC

Phase 3 (Jan-Mar 2027):   13 weeks
├─ Weeks 1-4: Block list + Terraform IaC
├─ Weeks 1-6: Performance benchmarking
├─ Weeks 7-13: Multi-region & DR
├─ Weeks 9-13: Customer guidelines & docs
└─ Deliverable: v3.0.0 with block list & IaC

Total: 34 weeks (was 39, optimized via parallelization)
```

### Version Milestones (Updated)

| Version | Release Date | Major Features | Status |
|---------|--------------|----------------|--------|
| **v1.0.0** | Sep 2026 | Phase 1 + Docker + Observability | Planned |
| **v1.1.0** | Oct 2026 | KMS integration phase 1 | Planned |
| **v2.0.0** | Dec 2026 | Events, Kafka CDC, Performance testing | Planned |
| **v2.1.0** | Jan 2027 | Performance optimizations | Planned |
| **v3.0.0** | Mar 2027 | Block list, Terraform, Customer guidelines | Planned |

---

## <a name="resources"></a>Resource Requirements (Updated)

### Phase 1 (7 weeks): 3 engineers
- 1 Backend Engineer (crypto + partitioning)
- 1 DevOps/Infrastructure (Docker Compose, VictoriaMetrics)
- 1 QA (load testing, observability validation)

### Phase 2 (14 weeks): 4-5 engineers
- 1 Backend Engineer - KMS/encryption lead
- 1 Backend Engineer - Events/Kafka CDC
- 1 Performance Engineer - Benchmarking
- 1 DevOps Engineer
- 1 QA Engineer

### Phase 3 (13 weeks): 4 engineers
- 1 Backend Engineer - Block list implementation
- 1 DevOps/Infrastructure - Terraform specialist
- 1 Performance Engineer - Scale benchmarking
- 1 QA/Documentation - Customer guides

---

## Summary of Updates

**Phase 1 Additions**:
- ✅ Database partitioning (hot/warm/cold tiers)
- ✅ Docker Compose with VictoriaMetrics
- ✅ Observability dashboards for local development
- ✅ Load testing framework

**Phase 2 Additions**:
- ✅ Event definitions & specifications (5 event types)
- ✅ Kafka integration for event streaming
- ✅ PostgreSQL CDC for data lake replication
- ✅ Performance testing framework (10K+ ops/sec)
- ✅ Horizontal scaling validation

**Phase 3 Additions**:
- ✅ Block list capability (Bloom filter for O(1) lookup)
- ✅ AWS Terraform IaC (complete multi-region infrastructure)
- ✅ Peak load benchmarking (100K+ ops/sec)
- ✅ Customer deployment guidelines
- ✅ Compliance guidance (SOC 2 & PCI DSS)

**Deployment Model**:
- ✅ PII Vault provides security foundation + compliance guidance
- ✅ Customers implement SOC 2 Type II certification themselves
- ✅ PCI DSS shared responsibility model clearly defined
- ✅ Customer compliance playbooks & checklists provided

**Timeline**: 34 weeks total (optimized via parallelization and moving certification to customers)

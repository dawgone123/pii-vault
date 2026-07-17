# Claude.md - PII Vault Project Guide

**Last Updated**: July 10, 2026  
**Project**: PII Vault - Secure PII Management System  
**Team**: dawgone123

---

## Project Overview

PII Vault is a Spring Boot 4.0.0 application for secure, encrypted storage and management of Personally Identifiable Information (PII). The system uses AWS KMS (via LocalStack in development) for cryptographic operations, ensuring all encryption is delegated to a key management service rather than handled locally.

**Key Principles**:
- **KMS-Only Encryption**: No local crypto - all encryption/decryption via AWS KMS
- **Audit Everything**: All operations logged for compliance
- **Metrics-First**: Built-in observability with VictoriaMetrics + Grafana
- **Zero-Config Development**: LocalStack mocks AWS services locally
- **PostgreSQL Partitioning**: Time-based partitions for operational efficiency

---

## Repository Structure

```
pii-vault/
├── src/
│   ├── main/
│   │   ├── java/com/pii/
│   │   │   ├── PiiVaultApplication.java          # Spring Boot entry point
│   │   │   ├── controller/PiiVaultController.java # REST API endpoints
│   │   │   ├── service/
│   │   │   │   ├── EncryptionService.java        # KMS encryption wrapper
│   │   │   │   └── PiiVaultService.java          # Business logic
│   │   │   ├── repository/PiiDataRepository.java # JPA repository
│   │   │   ├── model/PiiData.java                # Entity model
│   │   │   └── dto/
│   │   │       ├── PiiDataRequest.java
│   │   │       └── PiiDataResponse.java
│   │   └── resources/
│   │       └── application.yml                   # Spring config
│   └── test/
│       └── java/com/pii/
│           └── PiiVaultApplicationTests.java
├── docker/
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── datasources/victoriametrics.yaml
│   │   │   └── dashboards/
│   │   │       ├── dashboards.yaml
│   │   │       ├── operations-dashboard.json
│   │   │       └── performance-dashboard.json
│   │   └── README.md
│   └── OBSERVABILITY_SETUP.md
├── scripts/
│   ├── init-extensions.sql                      # PostgreSQL extensions
│   └── init-db.sql                              # Database schema
├── docs/
│   ├── GETTING_STARTED.md
│   ├── DOCKER_SETUP.md
│   ├── CONTAINER_DEPENDENCIES.md                # Service architecture
│   └── LIBRARY_VERSIONS.md                      # Dependency tracking
├── podman-compose.yml                           # Service orchestration
├── pom.xml                                      # Maven dependencies
├── Dockerfile                                   # Multi-stage build
├── CLAUDE.md                                    # This file
├── CONTAINER_SETUP_SUMMARY.md
├── OBSERVABILITY_CHANGES.md
└── BUILD_RESOLUTION.md
```

---

## Technology Stack

### Core Framework
- **Java**: 25 (LTS)
- **Spring Boot**: 4.0.0 (Latest LTS)
- **Spring Security**: Latest (via Boot parent)
- **Spring Data JPA**: Latest (Hibernate ORM)

### Database
- **PostgreSQL**: 18.1 (production & development)
- **JDBC Driver**: 42.7.5
- **Extensions**: uuid-ossp, pgcrypto, pg_stat_statements, pg_partman

### AWS & Cryptography
- **AWS SDK v2**: 2.29.23
- **AWS KMS**: For all encryption/decryption operations
- **LocalStack**: 0.14.2 (development AWS mock)

### Observability
- **Micrometer**: Metrics collection
- **Spring Boot Actuator**: Metrics endpoints
- **VictoriaMetrics**: Time-series storage (latest)
- **Grafana**: Dashboards (latest)

### Testing & Quality
- **JUnit 5**: Test framework
- **Spring Boot Test**: Integration testing
- **Testcontainers**: 1.20.2 (LocalStack, PostgreSQL)
- **Maven**: 3.9.9 (build tool)

### Containers
- **Podman/Docker**: Container runtime
- **Image Base**: eclipse-temurin:25-jdk-noble

---

## Development Workflow

### Setup
```bash
# Clone and navigate
git clone https://github.com/dawgone123/pii-vault.git
cd pii-vault

# Start all services (LocalStack, PostgreSQL, VictoriaMetrics, Grafana, PII Vault)
podman-compose up

# Verify services are healthy
podman-compose ps
```

### Build
```bash
# Compile only
mvn clean compile

# Full build with tests (creates JAR)
mvn clean install

# Build JAR only (skips tests, faster)
mvn clean package -DskipTests

# Build and run app locally (for testing before Docker)
mvn clean spring-boot:run
```

### Running PII Vault
```bash
# Via Maven (direct JVM)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Via Docker (after building JAR)
# First build JAR: mvn clean package -DskipTests
# Then build image: podman build -t pii-vault:latest .
# Then run container: podman-compose up -d

# View app logs
podman-compose logs -f pii-vault

# Check app health
curl http://localhost:8080/api/actuator/health

# View app metrics
curl http://localhost:8080/api/actuator/prometheus | grep pii_vault
```

### Testing
```bash
# Run all unit tests
mvn test

# Run specific test
mvn test -Dtest=PiiVaultApplicationTests

# Integration tests (uses Testcontainers + LocalStack)
mvn verify

# Full build with all tests
mvn clean verify
```

### Docker & Containers
```bash
# Build JAR locally
mvn clean package -DskipTests

# Build Docker image from JAR
podman build -t pii-vault:latest .

# Start entire stack (all 5 services)
podman-compose up -d

# Check all services running
podman-compose ps

# View logs for specific service
podman-compose logs -f pii-vault
podman-compose logs -f localstack
podman-compose logs -f postgres

# Stop services (keep data)
podman-compose stop

# Stop and remove containers (remove data volumes)
podman-compose down -v
```

---

## Project Phases & Milestones

### Phase 1: Foundation Hardening (Q3 2026)
**Goal**: Establish secure, tested KMS-only encryption architecture

**Week 1 (July 2026)**:
- ✅ Environment setup (Java 25, Spring Boot 4.0.0, PostgreSQL 18)
- ✅ Observability stack (VictoriaMetrics + Grafana)
- ✅ Container orchestration (Podman Compose)
- 🔄 **IN PROGRESS**: KMS-only encryption implementation
- 📋 Unit tests for encryption flow
- 📋 Integration tests with LocalStack

**Week 2-4**:
- Partition strategy implementation (pg_partman)
- Latency SLO validation (p95 < 50ms)
- Load testing (1000+ ops/sec target)
- Security hardening (input validation, SQL injection prevention)

### Phase 2: Advanced Features (Q4 2026)
- Event streaming (Kafka for audit events)
- CDC pipeline (Change Data Capture)
- Multi-region replication
- Compliance reporting (SOC 2, PCI-DSS)

### Phase 3: Production Hardening (Q1 2027)
- Multi-region failover
- Disaster recovery procedures
- Scaling & performance optimization
- Production deployment runbook

---

## Coding Standards

### Java Code Style
- **Package Structure**: `com.pii.<feature>.service|controller|repository|model`
- **Naming**: CamelCase classes, camelCase methods/variables, UPPER_SNAKE_CASE constants
- **Comments**: Minimal - only explain WHY, not WHAT (code should be self-documenting)
- **Error Handling**: Checked exceptions for recoverable errors, unchecked for programming errors

### Example Service
```java
package com.pii.service;

import org.springframework.stereotype.Service;

@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    
    // KMS-only: no local keys generated
    public String encrypt(String plainText) {
        // Delegate to AWS KMS via client
    }
    
    public String decrypt(String encryptedText) {
        // Delegate to AWS KMS via client
    }
}
```

### Database Naming
- **Tables**: `snake_case` (e.g., `pii_data`, `audit_logs`)
- **Columns**: `snake_case` (e.g., `created_at`, `pii_token_id`)
- **Indexes**: `idx_<table>_<column>` (e.g., `idx_pii_data_owner`)
- **Constraints**: `fk_<table>_<ref>` for foreign keys

### Configuration
- **Environment Variables**: Uppercase with underscores (e.g., `SPRING_DATASOURCE_URL`)
- **YAML Keys**: lowercase with dots (e.g., `spring.datasource.url`)
- **Profiles**: `dev`, `staging`, `prod`

---

## Dependency Management

### Maven Practices
- **Spring Boot Parent POM**: 4.0.0 (defines all transitive versions)
- **Explicit Versions Only For**:
  - AWS SDK (needs specific version for compatibility)
  - Testcontainers (explicitly pinned to 1.20.2)
  - PostgreSQL driver (pinned for PostgreSQL 18+)
- **No Custom Version Properties**: Let Spring Boot manage Jackson, Hibernate, etc.
- **Enforcer Plugin**: Prevents accidental gson, fastjson, or other JSON conflicts

### Adding Dependencies
1. Check if Spring Boot already manages it (search `spring-boot-dependencies`)
2. If yes, add without `<version>` tag
3. If no, add with pinned LTS version
4. Update `docs/LIBRARY_VERSIONS.md`
5. Run `mvn clean compile` to verify resolution

### Current Policy
```xml
<!-- ✅ DO: Let Spring Boot manage Jackson -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- ❌ DON'T: Override Spring Boot's management -->
<property>
    <jackson.version>2.18.0</jackson.version>
</property>

<!-- ✅ DO: Explicitly version if Spring Boot doesn't manage -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>kms</artifactId>
    <version>2.29.23</version>
</dependency>
```

---

## Documentation Files

### Files You Should Know
- **CLAUDE.md** (this file) - AI collaboration guidelines
- **CONTAINER_DEPENDENCIES.md** - Service architecture & startup ordering
- **CONTAINER_SETUP_SUMMARY.md** - Quick setup & troubleshooting
- **DOCKER_SETUP.md** - Detailed Docker/Podman instructions
- **GETTING_STARTED.md** - For first-time developers
- **LIBRARY_VERSIONS.md** - Dependency version tracking
- **BUILD_RESOLUTION.md** - Jackson version conflict resolution (historical)
- **OBSERVABILITY_CHANGES.md** - Observability implementation details

### When to Update
- **Architecture Change** → Update `CONTAINER_DEPENDENCIES.md`
- **Adding Dependencies** → Update `LIBRARY_VERSIONS.md`
- **Breaking Changes** → Add new doc or create `*_RESOLUTION.md` file
- **Observability Feature** → Update `docker/OBSERVABILITY_SETUP.md`

---

## Git Workflow

### Branch Strategy
- **Main Branch**: `main` - Production-ready code, tagged releases
- **Development Branch**: `v1` - Current development (what you're working on)
- **Feature Branches**: Not used for solo development (direct commits to `v1`)

### Commit Message Format
```
[type]: Brief description (under 70 chars)

Detailed explanation of changes:
- Bullet point 1
- Bullet point 2

Fixes: #ISSUE_NUMBER (if applicable)

Co-Authored-By: dawgone123 <anujrjaiswal@outlook.com>
Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>
```

**Note**: All commits must include both authors since this is collaborative work between AJ and Claude.

### Types
- `feat:` - New feature
- `fix:` - Bug fix
- `refactor:` - Code restructuring (no behavior change)
- `docs:` - Documentation only
- `test:` - Test additions/improvements
- `chore:` - Build, dependency updates, tooling

### Two-Author Convention (Critical)
⚠️ **All commits must include both authors**:
```
Co-Authored-By: dawgone123 <anujrjaiswal@outlook.com>
Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>
```

This reflects the collaborative nature of development:
- AJ handles decision-making, requirements, code review, and deployment
- Claude implements features, fixes bugs, and maintains code quality
- Every commit is a joint effort

**Do not omit either author** - this is the project convention.

### Examples
```bash
# Good commit
git commit -m "feat: Add KMS-only encryption service

- Implement EncryptionService delegating to AWS KMS
- Add LocalStack integration for development
- Update EncryptionService tests with mock KMS

Co-Authored-By: dawgone123 <anujrjaiswal@outlook.com>
Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"

# Good squash for multi-commit features
git rebase -i main
# Then squash intermediate commits

# Using git commit with HEREDOC for proper formatting
git commit -m "$(cat <<'EOF'
feat: Add KMS-only encryption service

- Implement EncryptionService delegating to AWS KMS
- Add LocalStack integration for development
- Update EncryptionService tests with mock KMS

Co-Authored-By: dawgone123 <anujrjaiswal@outlook.com>
Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>
EOF
)"
```

### Push Rules
- Always push to `v1` branch
- Run `mvn clean compile` before pushing
- Verify `podman-compose config` passes
- Never force-push to shared branches (only if explicitly authorized)

---

## Testing Strategy

### Test Types
1. **Unit Tests** (`src/test/java`)
   - Test single class/method in isolation
   - Mock all dependencies
   - Use `@Test` annotation
   - Run: `mvn test`

2. **Integration Tests** (uses Testcontainers)
   - Test multiple components together
   - Use real PostgreSQL/LocalStack (containers)
   - Use `@SpringBootTest`
   - Run: `mvn verify`

3. **Load Tests** (manual, ad-hoc)
   - Generate encryption operations
   - Monitor metrics in Grafana
   - Validate SLO: p95 latency < 50ms, throughput > 1000 ops/sec

### Test File Location
- Unit tests: `src/test/java/com/pii/<module>/<Class>Test.java`
- Integration tests: `src/test/java/com/pii/<module>/<Class>IT.java` (IT = Integration Test)

### Running Tests
```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=EncryptionServiceTest

# Integration tests only
mvn verify

# Skip tests (not recommended)
mvn clean install -DskipTests
```

---

## Observability & Monitoring

### Metrics Collection
Application automatically collects and exports metrics via:
- **Endpoint**: `http://localhost:8080/actuator/prometheus`
- **Format**: Prometheus text format
- **Frequency**: Every 15 seconds (pushed to VictoriaMetrics)

### Key Metrics
```
pii_vault_encryption_operations_total     # Counter: total encrypt ops
pii_vault_decryption_operations_total     # Counter: total decrypt ops
pii_vault_encryption_latency_seconds      # Histogram: encryption latency
pii_vault_encryption_errors_total         # Counter: failed encryptions
http_requests_total                       # HTTP requests
http_request_duration_seconds             # Request latency
jvm_memory_used_bytes                     # JVM heap usage
jvm_threads_live_threads                  # Live thread count
```

### Accessing Metrics
```bash
# View all metrics
curl http://localhost:8080/actuator/metrics

# View specific metric
curl http://localhost:8080/actuator/metrics/pii_vault.encryption.latency

# View Prometheus format
curl http://localhost:8080/actuator/prometheus | grep pii_vault
```

### Grafana Access
- **URL**: `http://localhost:3000`
- **Username**: `admin`
- **Password**: `admin`
- **Dashboards**: PII Vault (Operations & Performance)

### Alerts to Set (Phase 2)
- Encryption latency p95 > 50ms
- Error rate > 0.1%
- Database connection pool > 80%
- JVM memory > 70%

---

## Common Tasks & How-Tos

### Add a New Endpoint
1. Create DTO in `src/main/java/com/pii/dto/`
2. Add method to `PiiVaultController`
3. Add business logic to `PiiVaultService`
4. Add tests in `src/test/java/`
5. Update API documentation
6. Commit with `feat: Add <endpoint> endpoint`

### Add a Database Column
1. Update entity in `src/main/java/com/pii/model/PiiData.java`
2. Hibernate will auto-migrate (via `spring.jpa.hibernate.ddl-auto=update`)
3. Add index in `scripts/init-db.sql` if needed
4. Update `docs/` with schema changes
5. Commit with `feat: Add <column> to pii_data table`

### Debug KMS Issues
```bash
# Check LocalStack KMS is running
curl http://localhost:4566/

# Check KMS keys exist
podman exec pii-vault-localstack awslocal kms list-keys

# View LocalStack logs
podman-compose logs localstack

# Check application KMS connection
curl http://localhost:8080/actuator/health
```

### Performance Troubleshooting
1. Access Grafana: `http://localhost:3000`
2. View "Operations Dashboard"
3. Check latency percentiles (p50/p95/p99)
4. Check error rate
5. Check database connections
6. If slow: check PostgreSQL slow query log
   ```sql
   SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;
   ```

### Generate Test Load
```bash
# Simple load test (100 encrypt operations)
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/v1/encrypt \
    -H "Content-Type: application/json" \
    -d "{\"piiData\":\"test$i@example.com\",\"requestId\":\"test-$i\"}" &
done
wait

# Then view metrics in Grafana dashboards
```

---

## Troubleshooting Guide

### Build Fails
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/

# Recompile
mvn clean compile

# If still fails, check recent commits
git log --oneline -10
```

### Services Won't Start
```bash
# Check if ports are in use
lsof -i :8080
lsof -i :5432
lsof -i :3000

# Remove and recreate
podman-compose down -v
podman-compose up
```

### Database Connection Refused
```bash
# Check PostgreSQL is healthy
podman-compose ps postgres

# View logs
podman-compose logs postgres

# Wait longer for health check
sleep 30 && podman-compose ps
```

### Metrics Not Appearing
```bash
# Check Actuator is enabled
curl http://localhost:8080/actuator

# Check metrics endpoint
curl http://localhost:8080/actuator/prometheus | head -20

# Verify VictoriaMetrics is receiving data
curl 'http://localhost:8428/api/v1/query?query=pii_vault_encryption_operations_total'
```

### PostgreSQL Partitioning Not Working
```sql
-- Check if pg_partman is loaded
SELECT * FROM pg_extension WHERE extname = 'pg_partman';

-- Check partition table
SELECT schemaname, tablename FROM pg_tables 
WHERE tablename LIKE 'pii_data%';

-- Create partition manually if needed
SELECT partman.create_parent(
  'public.pii_data',
  'created_date',
  'monthly',
  '2026-07-01'
);
```

---

## Important Notes for Future Development

### KMS-Only Architecture
⚠️ **Critical**: All encryption MUST go through AWS KMS (LocalStack in dev).
- Never generate keys locally
- Never encrypt with local crypto libraries
- Always use `EncryptionService` which delegates to KMS
- This ensures compliance and key rotation support

### Database Partitioning Strategy
📋 `pii_data` table uses pg_partman for automatic monthly partitions:
- Old partitions can be archived to S3 (Phase 2+)
- Improves query performance on large datasets
- Maintenance handled by background worker (`pg_partman_bgw`)

### Metrics for SLO Validation
📊 Target SLOs (validate weekly):
- **Encryption Latency**: p95 < 50ms (monitor in Grafana)
- **Throughput**: > 1000 ops/sec under sustained load
- **Error Rate**: < 0.1% (0 acceptable in normal operation)
- **Availability**: 99.9% uptime

### Security Practices
🔒 **Always**:
- Use prepared statements (Hibernate handles this)
- Validate all user input
- Log security-relevant events (audit trail)
- Never log PII or encryption keys
- Use HTTPS in production (not configured for dev)
- Rotate KMS keys regularly (Phase 2+)

### Compliance Considerations
⚖️ **Phase 2 Requirements**:
- SOC 2 Type II readiness (audit logging complete)
- PCI-DSS compliance (if storing credit cards)
- GDPR compliance (right to be forgotten - via soft deletes)
- Data retention policies (implement in Phase 2)

---

## Contact & Support

### Communication
- **GitHub**: @dawgone123
- **Primary**: GitHub issues/discussions
- **Status Updates**: Commit messages & documentation

### When to Ask Claude
✅ **Appropriate Tasks**:
- "Add a new endpoint for X functionality"
- "Implement KMS encryption for the encryption service"
- "Set up partitioning for pii_data table"
- "Debug why test X is failing"
- "Optimize query Y that's running slow"
- "Create load test to validate SLOs"

❌ **Not Appropriate**:
- Architectural decisions without team consensus
- Breaking changes without prior discussion
- Risky operations (force push, deletion) without explicit approval
- Decisions affecting multiple teams/systems

---

## Quick Reference

### Essential Commands
```bash
# Development
podman-compose up                     # Start all services
mvn clean compile                     # Build
mvn test                             # Test
mvn spring-boot:run                  # Run app

# Monitoring
curl http://localhost:8080/health    # App health
curl http://localhost:8080/actuator/prometheus | grep pii_vault  # Metrics
# Grafana: http://localhost:3000

# Database
podman exec pii-vault-postgres psql -U vault_user -d pii_vault

# Cleanup
podman-compose down -v               # Stop & remove volumes
git clean -fd                        # Remove untracked files
rm -rf ~/.m2/repository              # Clear Maven cache
```

### Key File Locations
| Purpose | File |
|---------|------|
| Spring Boot Config | `src/main/resources/application.yml` |
| Database Schema | `scripts/init-db.sql` |
| Encryption Service | `src/main/java/com/pii/service/EncryptionService.java` |
| REST Controller | `src/main/java/com/pii/controller/PiiVaultController.java` |
| Docker Compose | `podman-compose.yml` |
| Container Docs | `docs/CONTAINER_DEPENDENCIES.md` |

---

## Version History

| Date | Change | Commit |
|------|--------|--------|
| 2026-07-08 | Initial v1 setup (Java 25, Spring Boot 4.0.0) | eb475e5 |
| 2026-07-09 | Observability stack (VictoriaMetrics + Grafana) | 68d0c4e |
| 2026-07-10 | Container dependencies & PostgreSQL extensions | 5bae5cf |
| 2026-07-10 | Add CLAUDE.md | Current |

---

**Last Reviewed**: July 10, 2026  
**Next Review**: August 10, 2026  
**Maintained By**: @dawgone123  
**Status**: ✅ Current & Complete


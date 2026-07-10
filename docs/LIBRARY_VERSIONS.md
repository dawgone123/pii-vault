# PII Vault Library & Dependency Versions

**Last Updated**: July 2026  
**Target**: Latest LTS versions across all dependencies

---

## Build & Runtime

### Java
- **Version**: 25 (Latest LTS)
- **Source**: OpenJDK via Eclipse Temurin
- **Used in**:
  - Maven build image: `maven:3.9.9-eclipse-temurin-25`
  - Runtime image: `eclipse-temurin:25-jdk-noble`

### Maven
- **Version**: 3.9.9 (Latest stable)
- **Purpose**: Build tool and dependency management
- **Build image**: `maven:3.9.9-eclipse-temurin-25`

---

## Framework & Core Dependencies

### Spring Boot
- **Version**: 4.0.0 (Latest LTS)
- **Parent POM**: `org.springframework.boot:spring-boot-starter-parent:4.0.0`
- **Key Components**:
  - `spring-boot-starter-web`: REST API support
  - `spring-boot-starter-data-jpa`: Database ORM
  - `spring-boot-starter-security`: Authorization/authentication
  - `spring-boot-starter-validation`: Input validation
  - `spring-boot-starter-actuator`: Metrics & health endpoints

### Spring Framework
- **Version**: 7.0.0 (Latest LTS)
- **Managed by**: Spring Boot 4.0.0 parent POM

---

## Database & Drivers

### PostgreSQL Driver
- **Version**: 42.7.5 (Latest LTS for PostgreSQL 18+)
- **Scope**: Runtime (production deployments)
- **Purpose**: JDBC connection to PostgreSQL 18+
- **Artifact**: `org.postgresql:postgresql:42.7.5`

### PostgreSQL Server (Docker)
- **Version**: 18.1-alpine (Latest LTS)
- **Container**: `docker.io/library/postgres:18.1-alpine`
- **Features Used**:
  - pg_stat_statements for query monitoring
  - JSONB operations for flexible data storage
  - Range types for date partitioning
  - Generated columns for immutability
  - Advanced indexing (BRIN, GiST, GIN)

### H2 Database
- **Version**: Latest (via Spring Boot BOM)
- **Scope**: Test & development
- **Purpose**: In-memory database for testing

---

## AWS SDK & Integration

### AWS SDK v2
- **Version**: 2.29.23 (Latest LTS)
- **Purpose**: AWS service integration
- **Key Service**: KMS (Key Management Service)
- **Artifact**: `software.amazon.awssdk:kms:2.29.23`
- **Features**:
  - KMS encryption/decryption API
  - CloudTrail logging (Phase 2+)
  - Multi-region key replication (Phase 3+)

### LocalStack
- **Version**: 0.2.25 (AWS 4.14 compatible)
- **Purpose**: Local AWS services emulation
- **Artifacts**:
  - `cloud.localstack:localstack-utils:0.2.25` (test scope)
- **Services Emulated**:
  - KMS (Key Management Service)
  - CloudTrail (audit logging)
  - Logs (CloudWatch Logs)

### Testcontainers
- **Version**: 1.20.2 (Latest LTS)
- **Purpose**: Docker container management for integration tests
- **Artifacts**:
  - `org.testcontainers:testcontainers:1.20.2`
  - `org.testcontainers:localstack:1.20.2`
  - `org.testcontainers:junit-jupiter:1.20.2`
- **Scope**: Test only

---

## JSON Processing

### Jackson
- **Version**: 2.17.2 (Latest LTS)
- **Purpose**: JSON serialization/deserialization
- **Managed via**: Spring Boot 4.0.0 parent POM
- **Key Modules** (auto-included):
  - `jackson-databind`: Core JSON processing
  - `jackson-annotations`: Annotations for JSON mapping
  - `jackson-core`: Streaming parser/generator
  - `jackson-datatype-jsr310`: Java 8+ java.time support
- **Policy**: Jackson 2.17.x exclusive (no gson, fastjson, or org.json)
- **Note**: Managed by Spring Boot - no version override needed

---

## Observability & Metrics

### Micrometer
- **Version**: Latest (via Spring Boot BOM)
- **Purpose**: Metrics collection and export
- **Artifacts**:
  - `io.micrometer:micrometer-registry-prometheus`
- **Capabilities**:
  - Prometheus metrics export format
  - Histogram latency tracking
  - Tag-based metric organization
  - Timer and counter utilities

### VictoriaMetrics (Docker)
- **Version**: Latest (`:latest` tag)
- **Container**: `victoriametrics/victoria-metrics:latest`
- **Purpose**: Time-series metrics storage
- **Features**:
  - Prometheus API compatible
  - 30-day retention (configurable)
  - Efficient storage (~10x smaller than Prometheus)
  - Single-node deployment

### Grafana (Docker)
- **Version**: Latest (`:latest` tag)
- **Container**: `grafana/grafana:latest`
- **Purpose**: Metrics visualization and dashboards
- **Pre-configured**:
  - VictoriaMetrics datasource
  - Operations Dashboard
  - Performance Dashboard

---

## Testing & Quality Assurance

### JUnit
- **Version**: Latest (via Spring Boot test starter)
- **Framework**: JUnit 5 (Jupiter)
- **Scope**: Test

### Spring Boot Test
- **Version**: 4.0.0 (Latest LTS)
- **Features**:
  - `@SpringBootTest` for integration tests
  - Test context management
  - Mock bean support
  - Actuator test utilities

### Spring Security Test
- **Version**: Latest (via Spring Boot test starter)
- **Features**: Security testing utilities

---

## Container Images

### Official Base Images

| Image | Version | Purpose |
|-------|---------|---------|
| `maven` | 3.9.9-eclipse-temurin-25 | Build stage container |
| `eclipse-temurin` | 25-jdk-noble | Runtime base image |
| `postgres` | 18.1-alpine | PostgreSQL database |
| `localstack/localstack` | latest | AWS services mock |
| `victoriametrics/victoria-metrics` | latest | Metrics database |
| `grafana/grafana` | latest | Metrics visualization |

### Image Sizing

| Image | Size | Usage |
|-------|------|-------|
| Maven builder | ~700MB | Build only (not in final image) |
| Runtime base | ~400MB | Final Docker image |
| PII Vault JAR | ~59MB | Application code |
| PostgreSQL | ~50MB | Database |
| LocalStack | ~200MB | AWS services mock |
| VictoriaMetrics | ~30MB | Metrics database |
| Grafana | ~100MB | Dashboards |

---

## Version Management Strategy

### LTS Selection Criteria
1. **Stability**: Proven in production environments
2. **Long-term support**: 3+ year support commitments
3. **Security**: Regular security patches
4. **Compatibility**: Works with Java 25+ and Spring Boot 4.0.0

### Update Frequency
- **Java/Maven**: Annually (security releases)
- **Spring Boot**: Every 6 months (minor versions)
- **AWS SDK**: Quarterly (feature updates + security)
- **TestContainers**: As needed (backward compatible)
- **Docker images**: Automatically track latest LTS

### Dependency Exclusions
To maintain security and prevent dependency conflicts:

```xml
<!-- Excluded Libraries (enforce Jackson 3 exclusive) -->
- org.json:json
- com.google.code.gson:gson
- com.alibaba:fastjson
- com.alibaba:fastjson2
```

---

## Upgrade History

| Date | Changes |
|------|---------|
| 2026-07-08 | Initial setup with Java 25, Spring Boot 4.0.0, Jackson managed by Spring Boot |
| 2026-07-09 | Upgraded AWS SDK (2.28.0 → 2.29.23), Testcontainers (1.20.0 → 1.20.2), PostgreSQL driver (42.7.4 → 42.7.5), Docker images to latest LTS |
| 2026-07-09 | Fixed Jackson version management: use Spring Boot managed Jackson 2.17.2 instead of custom override |

---

## Version Compatibility Matrix

| Component | Version | Java 25 | Spring 4.0.0 | PostgreSQL 18 |
|-----------|---------|---------|--------------|--------------|
| Jackson | 2.17.2 (Spring-managed) | ✅ | ✅ | N/A |
| AWS SDK v2 | 2.29.23 | ✅ | ✅ | N/A |
| LocalStack | 0.2.23 | ✅ | ✅ | ✅ |
| Testcontainers | 1.20.2 | ✅ | ✅ | ✅ |
| PostgreSQL driver | 42.7.5 | ✅ | ✅ | ✅ |
| Micrometer | Latest (Spring-managed) | ✅ | ✅ | N/A |

---

## Security Considerations

### Dependency Scanning
All dependencies tracked for:
- CVE vulnerabilities
- License compliance (Apache 2.0, MIT preferred)
- Supply chain risks

### Maven Enforcer
Configuration ensures:
- No conflicting JSON libraries (Jackson 3 exclusive)
- Dependency tree cleanliness
- Version consistency across modules

### AWS Credentials
- **Development**: LocalStack with mock credentials (test/test)
- **Production**: AWS IAM roles (no credentials in code)

---

## References

- [Spring Boot 4.0.0 Release Notes](https://spring.io/blog/2024/11/28/spring-boot-4-0-0-available-now)
- [Java 25 Features](https://www.oracle.com/java/technologies/javase/jdk25-doc.html)
- [PostgreSQL 18 Documentation](https://www.postgresql.org/docs/18/)
- [AWS SDK for Java v2](https://github.com/aws/aws-sdk-java-v2)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Testcontainers](https://testcontainers.com/)
- [Jackson Databind](https://github.com/FasterXML/jackson-databind)
- [Micrometer Documentation](https://micrometer.io/)

---

**Maintained by**: PII Vault Engineering Team  
**Last Reviewed**: July 2026

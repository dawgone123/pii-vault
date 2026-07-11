# Container Dependencies & Architecture

**Last Updated**: July 10, 2026  
**Status**: ✅ Complete with proper health checks and dependency ordering

---

## Service Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                   Startup Sequence & Dependencies               │
└─────────────────────────────────────────────────────────────────┘

Level 1: Infrastructure (Independent services)
├─ LocalStack (AWS KMS emulation)
│  └ Port: 4566, 4571
│  └ Health: AWS KMS list-keys command
│  └ Volumes: localstack_data
│
├─ PostgreSQL 18.1 (Database + Extensions)
│  └ Port: 5432
│  └ Health: pg_isready check
│  └ Volumes: postgres_data
│  └ Extensions: uuid-ossp, pgcrypto, pg_stat_statements, pg_partman
│
└─ VictoriaMetrics (Metrics Storage)
   └ Port: 8428
   └ Health: HTTP /health endpoint
   └ Volumes: victoriametrics_data
   └ Retention: 30 days

Level 2: Observability (Depends on Level 1)
└─ Grafana (Metrics Visualization)
   └ Depends on: VictoriaMetrics (service_healthy)
   └ Port: 3000
   └ Health: HTTP /api/health endpoint
   └ Volumes: grafana_data

Level 3: Application (Depends on Levels 1 & 2)
└─ PII Vault Application (Spring Boot)
   └ Depends on:
      ├─ LocalStack (service_healthy)
      ├─ PostgreSQL (service_healthy)
      ├─ VictoriaMetrics (service_healthy)
      └─ Grafana (service_healthy)
   └ Port: 8080
   └ Builds from: Dockerfile (multi-stage)
   └ Network: pii-vault-network (bridge)
```

---

## Service Details

### 1. LocalStack - AWS Services Emulation

**Image**: `localstack/localstack:latest`  
**Container Name**: `pii-vault-localstack`  
**Ports**: 4566 (main), 4571 (reserved)

**Purpose**: Emulates AWS services locally for development without AWS credentials

**Environment Variables**:
```yaml
SERVICES: kms,cloudtrail,logs
DEBUG: "false"
DATA_DIR: /tmp/localstack/data
DOCKER_HOST: unix:///var/run/docker.sock
AWS_DEFAULT_REGION: us-east-1
AWS_ACCESS_KEY_ID: test
AWS_SECRET_ACCESS_KEY: test
```

**Volumes**:
- `localstack_data:/tmp/localstack` - Persistent AWS service state

**Health Check**:
- Command: `awslocal kms list-keys`
- Interval: 10s
- Timeout: 5s
- Retries: 5

**Services Enabled**:
- **KMS** (Key Management Service) - Encryption/decryption
- **CloudTrail** - Audit logging
- **Logs** (CloudWatch Logs) - Log aggregation

**Dependencies**: None (starts independently)

---

### 2. PostgreSQL - Data Storage & Extensions

**Image**: `docker.io/library/postgres:18.1` (Full version with extension support)  
**Container Name**: `pii-vault-postgres`  
**Port**: 5432

**Purpose**: Primary database with advanced partitioning and crypto extensions

**Environment Variables**:
```yaml
POSTGRES_DB: pii_vault
POSTGRES_USER: vault_user
POSTGRES_PASSWORD: vault_password_dev
POSTGRES_INITDB_ARGS: "-c shared_preload_libraries=pg_stat_statements,pg_partman_bgw"
```

**Shared Preload Libraries**:
- `pg_stat_statements` - Query performance monitoring
- `pg_partman_bgw` - Automatic partition management background worker

**Extensions Installed**:
```sql
CREATE EXTENSION "uuid-ossp"              -- UUID generation
CREATE EXTENSION pgcrypto                 -- Cryptographic functions
CREATE EXTENSION pg_stat_statements       -- Query performance
CREATE EXTENSION pg_partman               -- Partition management
```

**Volumes**:
- `postgres_data:/var/lib/postgresql/data` - Database persistence
- `./scripts/init-extensions.sql` - Extensions initialization (runs first)
- `./scripts/init-db.sql` - Database schema initialization (runs second)

**Health Check**:
- Command: `pg_isready -U vault_user -d pii_vault`
- Interval: 10s
- Timeout: 5s
- Retries: 5

**Key Tables**:
- `pii_vault_kms_keys` - KMS key management
- `pii_data` - Encrypted PII with partitioning support
- `audit_logs` - Operation audit trail

**Dependencies**: None (starts independently)

---

### 3. VictoriaMetrics - Time-Series Database

**Image**: `victoriametrics/victoria-metrics:latest`  
**Container Name**: `pii-vault-victoriametrics`  
**Port**: 8428

**Purpose**: Lightweight time-series metrics storage (Prometheus API compatible)

**Command Line Arguments**:
```
--storageDataPath=/storage           # Data persistence location
--httpListenAddr=:8428               # HTTP server listen address
--retentionPeriod=30d                # 30-day metrics retention
--maxInsertRequestSize=33554432      # 32MB max request (for metrics bursts)
```

**Volumes**:
- `victoriametrics_data:/storage` - Metrics data persistence

**Health Check**:
- Command: `wget --no-verbose --tries=1 --spider http://localhost:8428/health`
- Interval: 10s
- Timeout: 5s
- Retries: 5

**Features**:
- Prometheus API compatible (MetricsQL querying)
- ~10x more efficient storage than Prometheus
- Single-node deployment for development
- Automatic time-series compression

**Dependencies**: None (starts independently)

**Restart Policy**: `on-failure` - Auto-restart if crashed

---

### 4. Grafana - Metrics Visualization

**Image**: `grafana/grafana:latest`  
**Container Name**: `pii-vault-grafana`  
**Port**: 3000

**Purpose**: Dashboard and visualization platform for metrics from VictoriaMetrics

**Environment Variables**:
```yaml
GF_SECURITY_ADMIN_PASSWORD: admin          # Admin password
GF_SECURITY_ADMIN_USER: admin              # Admin username
GF_USERS_ALLOW_SIGN_UP: "false"            # Disable self-registration
GF_INSTALL_PLUGINS: grafana-piechart-panel # Pre-installed plugins
GF_PATHS_PROVISIONING: /etc/grafana/provisioning
```

**Volumes**:
- `grafana_data:/var/lib/grafana` - Grafana settings and dashboards
- `./docker/grafana/provisioning/datasources` - VictoriaMetrics datasource config
- `./docker/grafana/provisioning/dashboards` - Pre-loaded dashboard definitions

**Health Check**:
- Command: `wget --no-verbose --tries=1 --spider http://localhost:3000/api/health`
- Interval: 10s
- Timeout: 5s
- Retries: 5

**Pre-configured Dashboards**:
1. **Operations Dashboard**
   - Encryption throughput (ops/sec)
   - Encryption latency (p50/p95/p99)
   - Error rates
   - Database connections

2. **Performance Dashboard**
   - Request latency distribution
   - Request rate by endpoint
   - JVM memory usage
   - JVM thread count

**Dependencies**: 
- `victoriametrics` (service_healthy) - Must be healthy before starting

**Restart Policy**: `on-failure` - Auto-restart if crashed

**Access**:
- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin`

---

### 5. PII Vault Application - Spring Boot

**Container Name**: `pii-vault-app`  
**Port**: 8080

**Purpose**: Main application server for PII encryption/decryption

**Build**:
- Context: Repository root
- Dockerfile: Multi-stage build (Maven builder + Java runtime)
- Image: Built locally (not pulled from registry)

**Environment Configuration**:

**Spring Configuration**:
```yaml
SPRING_PROFILES_ACTIVE: dev
SPRING_JPA_HIBERNATE_DDL_AUTO: update
SPRING_JPA_SHOW_SQL: "false"
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT: org.hibernate.dialect.PostgreSQLDialect
```

**PostgreSQL Connection**:
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pii_vault
SPRING_DATASOURCE_USERNAME: vault_user
SPRING_DATASOURCE_PASSWORD: vault_password_dev
```

**AWS/KMS Configuration**:
```yaml
AWS_REGION: us-east-1
AWS_ACCESS_KEY_ID: test
AWS_SECRET_ACCESS_KEY: test
AWS_ENDPOINT_OVERRIDE_KMS: http://localstack:4566
SPRING_CLOUD_AWS_REGION_STATIC: us-east-1
SPRING_CLOUD_AWS_S3_ENDPOINT: http://localstack:4566
SPRING_CLOUD_AWS_KMS_ENDPOINT: http://localstack:4566
```

**Logging Configuration**:
```yaml
LOGGING_LEVEL_ROOT: INFO
LOGGING_LEVEL_COM_PII: DEBUG
LOGGING_LEVEL_SOFTWARE_AMAZON_AWSSDK: WARN
```

**Dependencies** (service_healthy required):
1. `localstack` - AWS KMS emulation
2. `postgres` - Database connectivity
3. `victoriametrics` - Metrics collection endpoint
4. `grafana` - Observability stack completeness

**Network**: `pii-vault-network` (bridge)

**Restart Policy**: `on-failure` - Auto-restart if crashed

---

## Network Architecture

**Network Name**: `pii-vault-network`  
**Type**: Bridge (isolated Docker network)

### Service Discovery (Docker DNS)
Each service is accessible via its container name:
- `localstack:4566` - LocalStack API
- `postgres:5432` - PostgreSQL
- `victoriametrics:8428` - VictoriaMetrics
- `grafana:3000` - Grafana
- `pii-vault:8080` - Application

From host machine, use `localhost`:
- `localhost:4566` - LocalStack
- `localhost:5432` - PostgreSQL
- `localhost:8428` - VictoriaMetrics
- `localhost:3000` - Grafana
- `localhost:8080` - Application

---

## Startup Sequence & Wait Conditions

### Automatic Startup Order
When running `podman-compose up`:

1. **Level 1 Starts Immediately** (parallel):
   - LocalStack
   - PostgreSQL (with extension initialization)
   - VictoriaMetrics

2. **Level 2 Waits for Level 1**:
   - Grafana waits for `victoriametrics:service_healthy`

3. **Level 3 Waits for All**:
   - PII Vault waits for:
     - `localstack:service_healthy`
     - `postgres:service_healthy`
     - `victoriametrics:service_healthy`
     - `grafana:service_healthy`

### Health Check Flow
```
Level 1 Services (checking health):
└─ LocalStack: awslocal kms list-keys → ✅ healthy
└─ PostgreSQL: pg_isready → ✅ healthy
└─ VictoriaMetrics: wget /health → ✅ healthy
   ↓
Level 2 Waits & Starts:
└─ Grafana: wget /api/health → ✅ healthy
   ↓
Level 3 Waits & Starts:
└─ PII Vault: (all dependencies healthy) → ✅ starts
   └─ Spring Boot initialization
   └─ Database migrations
   └─ LocalStack KMS connection test
   └─ Ready to accept requests
```

---

## Volume Persistence

| Volume | Service | Purpose | Path | Persistence |
|--------|---------|---------|------|-------------|
| `postgres_data` | PostgreSQL | Database files | `/var/lib/postgresql/data` | Persistent |
| `localstack_data` | LocalStack | AWS service state | `/tmp/localstack` | Persistent |
| `victoriametrics_data` | VictoriaMetrics | Metrics data | `/storage` | Persistent (30 days) |
| `grafana_data` | Grafana | Settings & dashboards | `/var/lib/grafana` | Persistent |

**Cleanup**:
```bash
# Remove all volumes (reset all data)
podman-compose down -v

# Remove specific volume
podman volume rm pii-vault-network_postgres_data
```

---

## Port Mappings

| Service | Container Port | Host Port | Purpose |
|---------|---|---|---|
| LocalStack | 4566 | 4566 | AWS API endpoint |
| LocalStack | 4571 | 4571 | Reserved |
| PostgreSQL | 5432 | 5432 | Database access |
| PII Vault | 8080 | 8080 | REST API |
| VictoriaMetrics | 8428 | 8428 | Metrics API |
| Grafana | 3000 | 3000 | Web UI |

---

## PostgreSQL Extensions Detail

### Extension: uuid-ossp
**Purpose**: Native UUID type support  
**Use Case**: PII token generation (pii_token_id)  
**Functions**: `uuid_generate_v4()`, `uuid_generate_v1()`

### Extension: pgcrypto
**Purpose**: Cryptographic functions  
**Use Case**: Key derivation, hashing support  
**Functions**: `pgp_sym_encrypt()`, `pgp_sym_decrypt()`, `digest()`

### Extension: pg_stat_statements
**Purpose**: SQL performance tracking  
**Use Case**: Query optimization and monitoring  
**View**: `pg_stat_statements` system view

### Extension: pg_partman
**Purpose**: Automatic partition management  
**Use Case**: Time-based table partitioning for `pii_data`  
**Background Worker**: `pg_partman_bgw` (runs continuously)  
**Functions**: `partman.create_parent()`, `partman.maintain_partition_proc()`

---

## Troubleshooting Container Issues

### Check Service Status
```bash
podman-compose ps
```

### View Service Logs
```bash
# All services
podman-compose logs -f

# Specific service
podman-compose logs -f postgres
podman-compose logs -f pii-vault
podman-compose logs -f victoriametrics
podman-compose logs -f grafana
```

### Test Health Checks Manually
```bash
# LocalStack
podman exec pii-vault-localstack awslocal kms list-keys

# PostgreSQL
podman exec pii-vault-postgres pg_isready -U vault_user -d pii_vault

# VictoriaMetrics
podman exec pii-vault-victoriametrics wget -q -O- http://localhost:8428/health

# Grafana
podman exec pii-vault-grafana wget -q -O- http://localhost:3000/api/health
```

### Common Issues

**Issue**: PostgreSQL fails to start with extension errors  
**Solution**: Ensure using `postgres:18.1` (not alpine variant). Alpine lacks pg_partman.

**Issue**: Grafana can't connect to VictoriaMetrics  
**Solution**: Verify datasource URL uses container name: `http://victoriametrics:8428`

**Issue**: PII Vault can't reach PostgreSQL  
**Solution**: Ensure `depends_on` has `postgres: condition: service_healthy`

**Issue**: Metrics not appearing in Grafana  
**Solution**: Verify `spring-boot-starter-actuator` in pom.xml and `/actuator/metrics` endpoint

---

## Performance Characteristics

### Memory Usage
| Service | Memory | Notes |
|---------|--------|-------|
| LocalStack | ~500MB | AWS services emulation |
| PostgreSQL | ~200MB | Database + indexes |
| VictoriaMetrics | ~50MB | Time-series storage |
| Grafana | ~100MB | Web interface |
| PII Vault | ~400MB | JVM + Spring Boot |
| **Total** | **~1.3GB** | Development stack |

### Disk Usage (24 hours)
| Service | Daily Growth | 30-day estimate |
|---------|---|---|
| PostgreSQL | Variable | Depends on data |
| VictoriaMetrics | ~100MB | ~3GB (auto-cleanup) |
| Grafana | ~5MB | ~150MB |
| LocalStack | ~10MB | ~300MB |

---

## References

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [PostgreSQL 18 Extensions](https://www.postgresql.org/docs/18/contrib.html)
- [pg_partman Documentation](https://pgpartman.readthedocs.io/)
- [VictoriaMetrics Docs](https://docs.victoriametrics.com/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)

---

**Maintained by**: PII Vault Engineering Team  
**Last Reviewed**: July 10, 2026

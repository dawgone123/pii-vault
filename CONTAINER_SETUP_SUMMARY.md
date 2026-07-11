# Container Dependencies Setup Summary

**Date**: July 10, 2026  
**Commit**: 5bae5cf  
**Status**: ✅ Complete - All container dependencies configured with proper health checks and startup ordering

---

## Overview

Implemented comprehensive container dependency management for the PII Vault development stack with proper health checks, service discovery, and startup sequencing. All services now properly wait for their dependencies to be healthy before initializing.

---

## Changes Made

### 1. PostgreSQL Image & Extensions Update

**Changed**: `postgres:18.1-alpine` → `postgres:18.1` (full image)

**Reason**: Alpine variant doesn't include pg_partman extension support

**New Shared Preload Libraries**:
```
shared_preload_libraries=pg_stat_statements,pg_partman_bgw
```

**Extensions Enabled**:
- `uuid-ossp` - Native UUID generation (pii_token_id support)
- `pgcrypto` - Cryptographic functions for key derivation
- `pg_stat_statements` - Query performance monitoring
- `pg_partman` - Automatic partition management for time-series data

### 2. PostgreSQL Initialization Scripts

**Created**: `scripts/init-extensions.sql`
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS pg_partman;
```

**Updated**: `podman-compose.yml` volume order
```yaml
volumes:
  - ./scripts/init-extensions.sql:/docker-entrypoint-initdb.d/00-extensions.sql
  - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/01-init.sql
```

Extensions initialize first (00-), then database schema (01-)

### 3. Container Health Checks & Restart Policies

**VictoriaMetrics**:
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8428/health"]
  interval: 10s
  timeout: 5s
  retries: 5
restart: on-failure
```

**Grafana**:
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/api/health"]
  interval: 10s
  timeout: 5s
  retries: 5
restart: on-failure
```

### 4. Service Dependency Ordering

**Dependency Tree**:
```
Level 1 (Independent, start in parallel):
├─ LocalStack (AWS KMS)
├─ PostgreSQL (Database)
└─ VictoriaMetrics (Metrics storage)

Level 2 (Depends on Level 1):
└─ Grafana → requires victoriametrics:service_healthy

Level 3 (Depends on all previous):
└─ PII Vault → requires all 4 services: service_healthy
   ├─ localstack:service_healthy
   ├─ postgres:service_healthy
   ├─ victoriametrics:service_healthy
   └─ grafana:service_healthy
```

**Updated PII Vault depends_on**:
```yaml
depends_on:
  localstack:
    condition: service_healthy
  postgres:
    condition: service_healthy
  victoriametrics:
    condition: service_healthy
  grafana:
    condition: service_healthy
```

### 5. VictoriaMetrics Configuration

**Added Command Arguments**:
```yaml
command:
  - "--storageDataPath=/storage"
  - "--httpListenAddr=:8428"
  - "--retentionPeriod=30d"
  - "--maxInsertRequestSize=33554432"    # 32MB max request
```

### 6. Grafana Configuration

**Added Environment Variables**:
```yaml
GF_SECURITY_ADMIN_USER: admin
GF_SECURITY_ADMIN_PASSWORD: admin
GF_USERS_ALLOW_SIGN_UP: "false"
GF_INSTALL_PLUGINS: grafana-piechart-panel
GF_PATHS_PROVISIONING: /etc/grafana/provisioning
```

---

## Startup Sequence

### Manual Startup
```bash
cd /Users/aj/Code/pii-vault
podman-compose up
```

### Expected Startup Flow
```
1. docker-compose detects no running services
2. Level 1 services start in parallel:
   - LocalStack initializes AWS KMS
   - PostgreSQL initializes with extensions (00-extensions.sql)
   - PostgreSQL initializes database schema (01-init.sql)
   - VictoriaMetrics initializes metrics storage

3. Level 1 health checks run:
   - LocalStack: awslocal kms list-keys ✅
   - PostgreSQL: pg_isready ✅
   - VictoriaMetrics: wget /health ✅

4. Level 2 service starts (depends on Level 1):
   - Grafana waits for victoriametrics:service_healthy
   - Grafana connects to VictoriaMetrics datasource
   - Grafana loads pre-configured dashboards
   - Grafana health check: wget /api/health ✅

5. Level 3 service starts (depends on all):
   - PII Vault waits for all 4 services
   - PII Vault connects to PostgreSQL
   - PII Vault connects to LocalStack KMS
   - PII Vault health check: application startup ✅
   - Ready to accept requests on :8080

Total startup time: ~30-60 seconds (depending on image pull cache)
```

---

## Service Access

### From Host Machine
| Service | URL | Credentials |
|---------|-----|-------------|
| PII Vault API | `http://localhost:8080` | N/A |
| PostgreSQL | `localhost:5432` | vault_user / vault_password_dev |
| LocalStack AWS | `http://localhost:4566` | test / test |
| VictoriaMetrics | `http://localhost:8428` | No auth |
| Grafana | `http://localhost:3000` | admin / admin |

### From Container Network
Each service accessible via container name:
```
postgres:5432
localstack:4566
victoriametrics:8428
grafana:3000
pii-vault:8080
```

---

## PostgreSQL Extensions Detail

### Extension: uuid-ossp
```sql
SELECT uuid_generate_v4();  -- Generate random UUID v4
```
**Used for**: `pii_data.pii_token_id` generation

### Extension: pgcrypto
```sql
SELECT digest('data', 'sha256');  -- Hash function
```
**Used for**: Key derivation and cryptographic operations

### Extension: pg_stat_statements
```sql
SELECT query, calls, mean_time FROM pg_stat_statements;
```
**Used for**: Query performance monitoring and optimization

### Extension: pg_partman
```sql
SELECT partman.create_parent(
  'public.pii_data',
  'created_date',
  'monthly',
  '2026-07-01'
);
```
**Used for**: Automatic time-based partitioning of `pii_data` table

---

## Volumes & Persistence

| Volume | Service | Purpose | Size (typical) |
|--------|---------|---------|---|
| `postgres_data` | PostgreSQL | Database files | ~500MB-2GB |
| `localstack_data` | LocalStack | AWS service state | ~50MB |
| `victoriametrics_data` | VictoriaMetrics | Time-series metrics | ~100MB/day |
| `grafana_data` | Grafana | Settings & dashboards | ~100MB |

**Cleanup Data**:
```bash
# Remove all volumes and start fresh
podman-compose down -v

# Recreate volumes with init scripts
podman-compose up
```

---

## Health Check Details

### LocalStack Health
```bash
awslocal kms list-keys
```
Tests KMS service availability

### PostgreSQL Health
```bash
pg_isready -U vault_user -d pii_vault
```
Tests database connectivity and readiness

### VictoriaMetrics Health
```bash
wget --no-verbose --tries=1 --spider http://localhost:8428/health
```
Tests metrics storage HTTP endpoint

### Grafana Health
```bash
wget --no-verbose --tries=1 --spider http://localhost:3000/api/health
```
Tests Grafana API availability

### PII Vault Health
```
Spring Boot application startup
```
Tests application server initialization

---

## Troubleshooting

### PostgreSQL starts but extensions fail
**Issue**: `ERROR: could not open extension control file`  
**Cause**: Using alpine variant which lacks extensions  
**Fix**: Use `postgres:18.1` (full image), not `postgres:18.1-alpine`  
**Verify**: Check `podman-compose.yml` line 32

### Grafana can't reach VictoriaMetrics
**Issue**: Datasource connection error in Grafana  
**Cause**: Wrong URL in datasource config  
**Fix**: Ensure datasource uses container name: `http://victoriametrics:8428`  
**File**: `docker/grafana/provisioning/datasources/victoriametrics.yaml`

### PII Vault can't reach PostgreSQL
**Issue**: Connection refused on :5432  
**Cause**: PostgreSQL health check failed or startup incomplete  
**Check**: `podman-compose logs postgres | grep error`  
**Fix**: Wait for health check to pass (green ✅ in logs)

### Services stuck in "Starting" state
**Issue**: Containers running but services not healthy  
**Cause**: Health check interval too short or service slow to start  
**Fix**: 
```bash
# View health check status
podman-compose ps

# View detailed logs
podman-compose logs -f SERVICE_NAME

# Wait longer and retry
sleep 30 && podman-compose ps
```

### Metrics not appearing in Grafana
**Issue**: VictoriaMetrics shows no data  
**Cause**: PII Vault not connected or not exporting metrics  
**Fix**: 
1. Verify Actuator configured: `curl http://localhost:8080/actuator/metrics`
2. Verify metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
3. Check PII Vault logs: `podman-compose logs pii-vault`

---

## Performance Monitoring

### Monitor Service Status
```bash
# Real-time status
podman-compose ps

# Watch continuously
watch 'podman-compose ps'
```

### Monitor Resource Usage
```bash
podman stats pii-vault-postgres pii-vault-victoriametrics pii-vault-grafana
```

### Monitor Container Logs
```bash
# All services (live)
podman-compose logs -f

# Specific service
podman-compose logs -f postgres
podman-compose logs -f pii-vault

# Recent logs only
podman-compose logs --tail 100 pii-vault
```

---

## Next Steps

1. **Test Full Stack**
   ```bash
   podman-compose up
   curl http://localhost:8080/health
   ```

2. **Verify Metrics**
   ```bash
   curl http://localhost:8080/actuator/prometheus | grep pii_vault
   ```

3. **Access Grafana**
   ```
   http://localhost:3000
   Login: admin/admin
   View dashboards: PII Vault Operations & Performance
   ```

4. **Load Testing**
   - Generate encryption operations
   - Verify metrics in Grafana
   - Monitor performance dashboards

5. **Phase 1 Implementation**
   - KMS-only encryption (LocalStack mock)
   - Partition management setup
   - Performance baseline testing

---

## Documentation

- **Full Container Architecture**: See [docs/CONTAINER_DEPENDENCIES.md](../docs/CONTAINER_DEPENDENCIES.md)
- **Docker Setup Guide**: See [docs/DOCKER_SETUP.md](../docs/DOCKER_SETUP.md)
- **Getting Started**: See [docs/GETTING_STARTED.md](../docs/GETTING_STARTED.md)
- **Library Versions**: See [docs/LIBRARY_VERSIONS.md](../docs/LIBRARY_VERSIONS.md)

---

## References

- [PostgreSQL 18 Docker Image](https://hub.docker.com/_/postgres)
- [pg_partman Documentation](https://pgpartman.readthedocs.io/)
- [LocalStack Documentation](https://docs.localstack.cloud/)
- [VictoriaMetrics Docs](https://docs.victoriametrics.com/)
- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)

---

**Status**: ✅ All containers configured with proper dependencies  
**Build**: ✅ mvn clean compile succeeds  
**Config**: ✅ podman-compose config validates  
**Ready for**: Phase 1 KMS integration testing

# PII Vault Observability Setup

## Overview

The PII Vault development environment now includes a complete observability stack with **VictoriaMetrics** and **Grafana** for real-time metrics collection and visualization.

## What's New

### Removed
- ❌ **Redis** - No longer needed for caching in Phase 1
  - Simplifies dev stack (fewer containers)
  - Reduces memory footprint
  - Focus on core encryption functionality

### Added
- ✅ **VictoriaMetrics** - Lightweight time-series database
  - Accepts Prometheus-format metrics from Spring Boot
  - ~10x more efficient than Prometheus
  - Runs at `http://localhost:8428`
  
- ✅ **Grafana** - Metrics visualization and dashboards
  - Pre-configured Operations Dashboard (throughput, latency, errors)
  - Pre-configured Performance Dashboard (JVM, requests, memory)
  - Runs at `http://localhost:3000`
  
- ✅ **Spring Boot Actuator + Micrometer**
  - Metrics collection from application
  - Prometheus format export
  - JVM and HTTP metrics automatically tracked

## Architecture

```
Spring Boot Application (Micrometer)
    ↓ (Prometheus format metrics)
    ├─ Exposed at: http://localhost:8080/api/actuator/metrics
    └─ Exposed at: http://localhost:8080/api/actuator/prometheus

VictoriaMetrics (http://localhost:8428)
    ↓ (Stores time-series data)
    └─ Retention: 30 days

Grafana (http://localhost:3000)
    ├─ Datasource: VictoriaMetrics
    ├─ Operations Dashboard
    └─ Performance Dashboard
```

## Services

### 1. VictoriaMetrics

**Purpose**: Collect and store metrics in time-series format

**Container Image**: `victoriametrics/victoria-metrics:latest`

**Endpoint**: `http://localhost:8428`

**Features**:
- Prometheus-compatible API
- 30-day retention (configurable)
- Single-node deployment
- Built-in query language (MetricsQL)

**Key Commands**:
```bash
# Query metrics
curl 'http://localhost:8428/api/v1/query?query=pii_vault_encryption_operations_total'

# Query rate (per second)
curl 'http://localhost:8428/api/v1/query_range?query=rate(pii_vault_encryption_operations_total[1m])'

# Health check
curl http://localhost:8428/health
```

### 2. Grafana

**Purpose**: Visualize metrics and provide dashboards

**Container Image**: `grafana/grafana:latest`

**Endpoint**: `http://localhost:3000`

**Default Credentials**:
- Username: `admin`
- Password: `admin`

**Key Features**:
- Pre-configured VictoriaMetrics datasource
- Operations Dashboard (built-in)
- Performance Dashboard (built-in)
- Real-time metric updates (10-second refresh)
- Alert capability

**Pre-Configured Dashboards**:

#### Operations Dashboard
Monitors PII Vault encryption operations:
- **Encryption Throughput** - ops/sec (target: >= 1000)
- **Encryption Latency** - p50/p95/p99 in ms (target: p95 < 50ms)
- **Error Rates** - encryption/decryption errors/sec
- **DB Connections** - active vs maximum connections

#### Performance Dashboard
Monitors application performance:
- **Request Latency** - p50/p95/p99 percentiles
- **Request Rate** - requests/sec by endpoint
- **JVM Memory** - heap utilization
- **JVM Threads** - live and peak thread count

### 3. Spring Boot Actuator + Micrometer

**Configuration** (in `src/main/resources/application.yml`):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
        pii.vault.encryption.latency: true
```

**Exposed Endpoints**:
- `/api/actuator/health` - Application health
- `/api/actuator/metrics` - Available metrics
- `/api/actuator/prometheus` - Prometheus-format metrics

**Auto-Collected Metrics**:
- HTTP request metrics (latency, rate, errors)
- JVM metrics (memory, GC, threads)
- Database connection pool metrics
- Custom encryption operation metrics

## Docker Compose Configuration

### Services Definition

```yaml
# VictoriaMetrics - Time-series database
victoriametrics:
  image: victoriametrics/victoria-metrics:latest
  container_name: pii-vault-victoriametrics
  ports:
    - "8428:8428"
  volumes:
    - victoriametrics_data:/storage
  command:
    - "--storageDataPath=/storage"
    - "--httpListenAddr=:8428"
    - "--retentionPeriod=30d"

# Grafana - Metrics visualization
grafana:
  image: grafana/grafana:latest
  container_name: pii-vault-grafana
  ports:
    - "3000:3000"
  environment:
    GF_SECURITY_ADMIN_PASSWORD: admin
    GF_USERS_ALLOW_SIGN_UP: "false"
  volumes:
    - grafana_data:/var/lib/grafana
    - ./docker/grafana/provisioning/datasources:/etc/grafana/provisioning/datasources
    - ./docker/grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards
```

## Getting Started

### 1. Start Services

```bash
# Navigate to project root
cd /Users/aj/Code/pii-vault

# Start all services (including VictoriaMetrics & Grafana)
podman-compose up -d

# Verify services are running
podman-compose ps

# Expected output shows:
# - pii-vault-app (running)
# - pii-vault-postgres (running)
# - pii-vault-localstack (running)
# - pii-vault-victoriametrics (running)
# - pii-vault-grafana (running)
```

### 2. Access Grafana Dashboards

```bash
# Open in browser
http://localhost:3000

# Login
Username: admin
Password: admin

# Navigate to Dashboards
- Click "Dashboards" in left menu
- Select "PII Vault" folder
- Click "Operations Dashboard" or "Performance Dashboard"
```

### 3. Perform Operations & Watch Metrics

```bash
# Encrypt some data (generates metrics)
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiData": "test@example.com",
    "requestId": "550e8400-e29b-41d4-a716-446655440000"
  }'

# Watch Grafana dashboard update in real-time
# Refresh every 10 seconds automatically
```

## Key Metrics to Monitor

### Encryption Performance
- **Metric**: `pii_vault_encryption_operations_total`
- **Query**: `rate(pii_vault_encryption_operations_total[1m])`
- **Target**: >= 1000 ops/sec during testing
- **Alert**: If < 100 ops/sec

### Encryption Latency
- **Metric**: `pii_vault_encryption_latency_seconds`
- **Query**: `histogram_quantile(0.95, rate(pii_vault_encryption_latency_seconds_bucket[5m]))`
- **Target**: p95 < 50ms
- **Alert**: If p95 > 100ms

### Error Rate
- **Metric**: `pii_vault_encryption_errors_total`
- **Query**: `rate(pii_vault_encryption_errors_total[1m])`
- **Target**: < 0.1% error rate
- **Alert**: If > 1% errors

### JVM Heap Memory
- **Metric**: `jvm_memory_used_bytes`
- **Query**: `jvm_memory_used_bytes{area="heap"} / 1024 / 1024`
- **Target**: < 80% of max heap
- **Alert**: If > 90% utilized

## Configuration Files

### Datasource Configuration
**File**: `docker/grafana/provisioning/datasources/victoriametrics.yaml`

Defines VictoriaMetrics as the Prometheus datasource for Grafana.

### Dashboard Provisioning
**File**: `docker/grafana/provisioning/dashboards/dashboards.yaml`

Configures where Grafana loads dashboard definitions.

### Dashboard Definitions
**Files**:
- `docker/grafana/provisioning/dashboards/operations-dashboard.json`
- `docker/grafana/provisioning/dashboards/performance-dashboard.json`

Pre-built dashboard JSON with all panels and queries.

## Troubleshooting

### VictoriaMetrics Not Receiving Metrics

```bash
# Check if app is exposing metrics
curl http://localhost:8080/api/actuator/metrics | grep pii_vault

# Check VictoriaMetrics health
curl http://localhost:8428/health

# View logs
podman-compose logs victoriametrics

# Restart
podman-compose restart victoriametrics
```

### Grafana Can't Connect to VictoriaMetrics

```bash
# Test connection from Grafana container
podman-compose exec grafana curl http://victoriametrics:8428/health

# Verify datasource in Grafana UI
# Admin → Data Sources → VictoriaMetrics → Test

# Restart Grafana
podman-compose restart grafana
```

### Dashboards Not Showing Data

```bash
# Check metrics exist in VictoriaMetrics
curl 'http://localhost:8428/api/v1/query?query=pii_vault_encryption_operations_total'

# Expected: {"status":"success","data":{"result":[...]}}

# If empty, run some operations to generate metrics
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{"piiData":"test@example.com","requestId":"test-123"}'

# Wait 15 seconds for metrics to push to VictoriaMetrics
# Then refresh Grafana dashboards
```

### Grafana Won't Start

```bash
# Check logs
podman-compose logs grafana

# Restart
podman-compose restart grafana

# If persistent, reset Grafana
podman-compose down
podman volume rm pii_vault_grafana_data
podman-compose up -d grafana

# Re-login with admin/admin
```

## Performance Impact

### Storage
- **VictoriaMetrics**: ~100MB for 24 hours of metrics
- **Grafana**: ~50MB for database and dashboards
- **Total**: ~150MB additional per container

### CPU/Memory
- **VictoriaMetrics**: ~50MB RAM, low CPU overhead
- **Grafana**: ~100MB RAM, minimal CPU when not viewing dashboards
- **Minimal impact** on PII Vault application performance

## Next Steps

1. **View dashboards** at http://localhost:3000
2. **Run load tests** to generate metrics
3. **Create custom dashboards** for your metrics
4. **Set up alerts** for critical thresholds
5. **Export metrics** for compliance/audit trails

## References

- [VictoriaMetrics Documentation](https://docs.victoriametrics.com/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
- [Micrometer Metrics](https://micrometer.io/)

---

**Last Updated**: July 2026

**Files Changed**:
- `podman-compose.yml` - Added VictoriaMetrics & Grafana, removed Redis
- `pom.xml` - Added Micrometer & Actuator dependencies
- `src/main/resources/application.yml` - Added Actuator/Metrics configuration
- `docker/grafana/provisioning/` - New Grafana configuration
- `docker/OBSERVABILITY_SETUP.md` - This file
- Documentation updates in `GETTING_STARTED.md` and `DOCKER_SETUP.md`

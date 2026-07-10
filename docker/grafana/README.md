# Grafana Configuration for PII Vault

This directory contains Grafana provisioning configuration for PII Vault observability dashboards.

## Structure

```
docker/grafana/
├── provisioning/
│   ├── datasources/
│   │   └── victoriametrics.yaml          # VictoriaMetrics datasource configuration
│   └── dashboards/
│       ├── dashboards.yaml               # Dashboard provisioning configuration
│       ├── operations-dashboard.json     # Operations metrics dashboard
│       └── performance-dashboard.json    # Performance metrics dashboard
└── README.md                             # This file
```

## Datasources

### VictoriaMetrics

The `victoriametrics.yaml` file configures Grafana to connect to VictoriaMetrics:

```yaml
datasources:
  - name: VictoriaMetrics
    type: prometheus
    access: proxy
    url: http://victoriametrics:8428
    isDefault: true
```

**Key Fields**:
- `type: prometheus` - VictoriaMetrics uses Prometheus API compatibility
- `access: proxy` - Grafana backend proxies requests to VictoriaMetrics
- `url: http://victoriametrics:8428` - Internal Docker network URL
- `isDefault: true` - Default datasource for all panels

## Dashboards

### Operations Dashboard

Monitors encryption operations and system health:

**Panels**:
1. **Encryption Throughput (ops/sec)** - Real-time encryption/decryption rate
2. **Encryption Latency (ms)** - Latency percentiles (p50, p95, p99)
3. **Error Rate** - Encryption/decryption error rates
4. **Database Connections** - Active vs max connections

**Metrics Used**:
- `pii_vault_encryption_operations_total` - Total encryption operations
- `pii_vault_decryption_operations_total` - Total decryption operations
- `pii_vault_encryption_latency_seconds_bucket` - Latency histogram
- `pii_vault_encryption_errors_total` - Encryption errors
- `pii_vault_decryption_errors_total` - Decryption errors
- `pii_vault_db_connections_active` - Active DB connections
- `pii_vault_db_connections_max` - Max DB connections

### Performance Dashboard

Monitors application and JVM performance:

**Panels**:
1. **Request Latency Percentiles (ms)** - HTTP request latency (p50, p95, p99)
2. **Request Rate (req/sec)** - Requests per second by method/path
3. **JVM Memory Usage (MB)** - Heap memory utilization
4. **JVM Thread Count** - Active and peak thread counts

**Metrics Used**:
- `http_request_duration_seconds_bucket` - HTTP request latency histogram
- `http_requests_total` - Total HTTP requests
- `jvm_memory_used_bytes` - JVM memory used
- `jvm_memory_max_bytes` - JVM memory maximum
- `jvm_threads_live_threads` - Live thread count
- `jvm_threads_peak_threads` - Peak thread count

## Customizing Dashboards

### Edit Dashboard JSON

1. **Edit File Directly**:
   ```bash
   # Edit the JSON file
   nano docker/grafana/provisioning/dashboards/operations-dashboard.json
   ```

2. **Update Grafana UI**:
   - Open Grafana at http://localhost:3000
   - Go to Dashboards → Operations Dashboard
   - Click "Edit" in top right
   - Make changes
   - Click "Save" to persist

3. **Restart to Apply File Changes**:
   ```bash
   podman-compose restart grafana
   ```

### Add New Metrics

To add a new panel for a custom metric:

1. **In JSON**:
   ```json
   {
     "id": 5,
     "title": "Custom Metric",
     "type": "graph",
     "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16},
     "targets": [
       {
         "expr": "rate(custom_metric_total[1m])",
         "refId": "A",
         "legendFormat": "{{label}}"
       }
     ]
   }
   ```

2. **Via Grafana UI**:
   - Open Dashboard in Edit mode
   - Click "Add Panel"
   - Select "Graph"
   - Enter metric query (MetricsQL)
   - Configure visualization
   - Save

## Accessing Grafana

```bash
# URL
http://localhost:3000

# Default Credentials
Username: admin
Password: admin

# First Login
1. Go to http://localhost:3000
2. Enter admin / admin
3. Change password when prompted (or skip)
4. Go to Dashboards → PII Vault folder
5. View metrics
```

## Adding New Dashboards

### From Grafana UI

1. Dashboard → Create → New Dashboard
2. Add panels and queries
3. Save with name: "PII Vault - <Dashboard Name>"
4. Export as JSON

### From JSON File

1. Create new JSON file in `provisioning/dashboards/`
2. Follow Grafana dashboard JSON format
3. Set `"overwrite": true` to auto-update
4. Restart Grafana: `podman-compose restart grafana`

### Dashboard JSON Template

```json
{
  "dashboard": {
    "id": null,
    "uid": "unique-dashboard-id",
    "title": "PII Vault - Dashboard Name",
    "tags": ["pii-vault"],
    "timezone": "browser",
    "schemaVersion": 27,
    "version": 1,
    "refresh": "10s",
    "panels": []
  },
  "overwrite": true
}
```

## Metrics from Spring Boot

Spring Boot Actuator exposes metrics via Micrometer. Custom metrics can be added:

```java
@Service
public class CustomMetricsService {
  private final MeterRegistry meterRegistry;
  
  public CustomMetricsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }
  
  public void recordCustomMetric(String metricName, long value) {
    meterRegistry.counter("custom_metric", "name", metricName).increment(value);
  }
}
```

These metrics are automatically exposed at:
- Prometheus format: `http://localhost:8080/api/actuator/prometheus`
- VictoriaMetrics pushes every 15 seconds

## Troubleshooting

### Grafana Can't Connect to VictoriaMetrics

```bash
# Check VictoriaMetrics is running
podman-compose ps victoriametrics

# Check health
curl http://localhost:8428/health

# View VictoriaMetrics logs
podman-compose logs victoriametrics

# Verify network connectivity
podman-compose exec grafana curl http://victoriametrics:8428/health
```

### Dashboards Not Showing Data

```bash
# Check if metrics are being collected
curl http://localhost:8080/api/actuator/metrics

# Check if metrics are in VictoriaMetrics
curl 'http://localhost:8428/api/v1/query?query=pii_vault_encryption_operations_total'

# View Grafana logs
podman-compose logs grafana
```

### Grafana Won't Start

```bash
# Check logs
podman-compose logs grafana

# Restart
podman-compose down grafana
podman-compose up -d grafana

# If issues persist, reset Grafana
podman-compose down
podman volume rm pii_vault_grafana_data
podman-compose up -d grafana
```

## References

- [Grafana Provisioning](https://grafana.com/docs/grafana/latest/administration/provisioning/)
- [Grafana Dashboard JSON Model](https://grafana.com/docs/grafana/latest/dashboards/manage-dashboards/)
- [VictoriaMetrics Grafana Plugin](https://grafana.com/grafana/plugins/victoriametrics-app/)
- [MetricsQL Query Language](https://docs.victoriametrics.com/metricsql/)
- [Prometheus Query Language (PromQL)](https://prometheus.io/docs/prometheus/latest/querying/basics/)

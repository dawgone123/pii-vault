# Local Development Guide - PII Vault

**Last Updated**: July 16, 2026  
**Purpose**: Step-by-step instructions for running PII Vault locally during development

---

## Quick Start (5 minutes)

### Prerequisites
- Java 25 installed
- Maven 3.9.9 installed
- Podman or Docker installed
- Git installed

### 1. Start Infrastructure (LocalStack, PostgreSQL, etc.)
```bash
# Start all services in background
podman-compose up -d

# Verify all services are running
podman-compose ps
```

Expected output shows 5 containers:
- `pii-vault-app` (not running yet, app will start separately)
- `pii-vault-postgres` (running)
- `pii-vault-localstack` (running)
- `pii-vault-victoriametrics` (running)
- `pii-vault-grafana` (running)

### 2. Build JAR (one-time)
```bash
# Build JAR without running tests (faster)
mvn clean package -DskipTests
```

This creates `target/pii-vault-1.0.0.jar` which is used by both Docker and direct Maven runs.

### 3. Run the Application
Choose one approach:

#### Option A: Run via Maven (direct JVM) - Recommended for active development
```bash
mvn spring-boot:run
```

This compiles and runs the app with live logs in your terminal. Press `Ctrl+C` to stop.

#### Option B: Run via Docker (containerized)
```bash
# Build Docker image from JAR
podman build -t pii-vault:latest .

# Start container via docker-compose
podman-compose up pii-vault
```

### 4. Verify App is Running
```bash
# Check app health
curl http://localhost:8080/api/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

---

## Building

### Build Options

#### Fast build (no tests, fastest)
```bash
mvn clean package -DskipTests
```
**Time**: ~10 seconds  
**Output**: JAR in `target/pii-vault-1.0.0.jar`  
**Use when**: You're confident in your changes and want to iterate quickly

#### Full build with unit tests
```bash
mvn clean install
```
**Time**: ~30 seconds  
**Output**: JAR + runs all unit tests  
**Use when**: You've changed core business logic

#### Full build with all tests (unit + integration)
```bash
mvn clean verify
```
**Time**: ~2 minutes  
**Output**: JAR + runs all tests including integration tests with Testcontainers  
**Use when**: You're ready to commit, testing with real LocalStack/PostgreSQL

#### Compile only (no JAR)
```bash
mvn clean compile
```
**Time**: ~5 seconds  
**Output**: Compiled classes in `target/classes`  
**Use when**: You just want to check for compilation errors

---

## Running the Application

### Run via Maven (Recommended for Development)

```bash
# Run with default profile (dev)
mvn spring-boot:run

# Run with explicit dev profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Run with custom arguments
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

**Advantages**:
- ✅ Recompiles on changes (if using IDE integration)
- ✅ Logs appear in terminal immediately
- ✅ Easy to restart (Ctrl+C → up arrow → Enter)
- ✅ Can pass system properties easily

**Output** - You should see logs like:
```
2026-07-16 10:30:45.123 INFO 12345 --- [main] com.pii.PiiVaultApplication : Starting PiiVaultApplication...
2026-07-16 10:30:46.456 INFO 12345 --- [main] org.springframework.boot.web.embedded.tomcat.TomcatWebServer : Tomcat initialized with port(s): 8080
2026-07-16 10:30:47.789 INFO 12345 --- [main] com.pii.PiiVaultApplication : Started PiiVaultApplication in 2.5s
```

### Run via Docker (Containerized)

```bash
# Start container via compose (runs in foreground, shows logs)
podman-compose up pii-vault

# Or start in background
podman-compose up -d pii-vault

# View logs after starting in background
podman-compose logs -f pii-vault
```

**Advantages**:
- ✅ Matches production environment exactly
- ✅ Isolated from host system
- ✅ Easy to manage with docker-compose

---

## Monitoring & Health Checks

### App Health Endpoint
```bash
# Check if app is running and healthy
curl http://localhost:8080/api/actuator/health
```

Response: `{"status":"UP"}`

### View Application Metrics
```bash
# Get all available metrics
curl http://localhost:8080/api/actuator/metrics

# View specific metric
curl http://localhost:8080/api/actuator/metrics/pii_vault.encryption.latency

# View Prometheus format (for VictoriaMetrics)
curl http://localhost:8080/api/actuator/prometheus | grep pii_vault
```

### Grafana Dashboards
```
URL: http://localhost:3000
Username: admin
Password: admin
```

Available dashboards:
- **Operations Dashboard**: Encryption operations, error rates
- **Performance Dashboard**: Latency p50/p95/p99, throughput

---

## Viewing Logs

### Maven Run Logs (in terminal)
When running via `mvn spring-boot:run`, logs appear directly in your terminal. Use grep to filter:

```bash
# Only app logs (skip framework logs)
mvn spring-boot:run | grep "com.pii"

# Only ERROR and WARN logs
mvn spring-boot:run | grep -E "ERROR|WARN"
```

### Container Logs
```bash
# View current logs
podman-compose logs pii-vault

# Follow logs in real-time
podman-compose logs -f pii-vault

# View last N lines
podman-compose logs --tail=50 pii-vault

# View logs with timestamps
podman-compose logs --timestamps pii-vault
```

### Database Logs
```bash
# PostgreSQL logs
podman-compose logs postgres

# LocalStack logs (AWS emulation)
podman-compose logs localstack
```

### All Services Logs
```bash
# View logs from all services
podman-compose logs -f
```

---

## Testing

### Unit Tests
```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=PiiVaultApplicationTests

# Run specific test method
mvn test -Dtest=PiiVaultApplicationTests#testHealthCheck

# Skip tests during build
mvn package -DskipTests
```

### Integration Tests
Requires LocalStack and PostgreSQL to be running:

```bash
# Run integration tests
mvn verify

# Run integration tests for specific class
mvn verify -Dtest=PiiVaultApplicationIT
```

### Test Output
Tests generate coverage reports in `target/site/jacoco/index.html` (after running `mvn verify`)

---

## Debugging

### Enable Debug Logging
Modify `src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.pii: DEBUG                    # Current setting
    org.springframework: DEBUG        # Add for Spring logs
    org.hibernate.SQL: DEBUG          # Add for SQL queries
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Add for SQL parameters
```

### IDE Debugging (IntelliJ/VS Code)
```bash
# Run with debug port open
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

Then attach debugger to `localhost:5005`.

### Print Statements
Add to any Java class:
```java
System.out.println("DEBUG: variable = " + variable);
```

Or use SLF4J logger:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.debug("DEBUG: variable = {}", variable);
```

---

## Database Access

### Connect to PostgreSQL
```bash
# Open PostgreSQL shell
podman exec -it pii-vault-postgres psql -U vault_user -d pii_vault

# Common SQL commands
SELECT * FROM pii_data LIMIT 10;           # View PII data
SELECT * FROM pg_stat_statements;          # View query stats
SELECT * FROM pg_partman.check_parent();   # View partitions
```

### View Database Logs
```bash
# PostgreSQL logs
podman-compose logs postgres
```

---

## Container Management

### Check Container Status
```bash
# List all containers (with status)
podman-compose ps

# Show more details
podman-compose ps -a
```

### Stop Services
```bash
# Stop all containers (data persists)
podman-compose stop

# Stop specific service
podman-compose stop postgres

# Stop and remove containers (data is deleted!)
podman-compose down -v
```

### Restart Services
```bash
# Restart all services
podman-compose restart

# Restart specific service
podman-compose restart postgres

# Restart app (useful after code changes with Docker run)
podman-compose restart pii-vault
```

### View Container Logs
```bash
# All services
podman-compose logs -f

# Specific service
podman-compose logs -f pii-vault
podman-compose logs -f postgres
podman-compose logs -f localstack
```

### Clean Up Everything
```bash
# Stop and remove all containers AND volumes
podman-compose down -v

# Remove dangling images
podman image prune -f

# Clear all PII Vault containers/images
podman system prune -a --volumes -f
```

---

## Common Development Workflows

### Workflow 1: Active Development (Fastest)
```bash
# Terminal 1: Start infrastructure
podman-compose up

# Terminal 2: Run app with Maven (shows live logs)
mvn spring-boot:run

# Make code changes, app auto-recompiles if using IDE integration
# Or manually: Ctrl+C to stop, up arrow to restart
```

### Workflow 2: Testing Changes
```bash
# Terminal 1: Start infrastructure
podman-compose up

# Terminal 2: Run tests (runs against real containers)
mvn verify

# Check results in terminal
```

### Workflow 3: Docker-Based Development
```bash
# Build JAR once
mvn clean package -DskipTests

# Terminal 1: Start entire stack (including app in Docker)
podman-compose up

# View logs
podman-compose logs -f pii-vault

# Make code changes, rebuild and restart
mvn clean package -DskipTests
podman-compose restart pii-vault
```

### Workflow 4: Debugging
```bash
# Terminal 1: Start infrastructure
podman-compose up

# Terminal 2: Run app with debug port open
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Terminal 3 (in IDE): Attach debugger to localhost:5005
# Set breakpoints and debug!
```

---

## API Endpoints Reference

### All Available Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/health` | GET | Health check (Spring Actuator) |
| `/api/metrics` | GET | List available metrics |
| `/api/prometheus` | GET | Prometheus format metrics |
| `/api/v1/encrypt` | POST | Encrypt single PII entry |
| `/api/v1/decrypt` | POST | Decrypt token to PII |

### Testing Endpoints

#### Health Check
```bash
curl http://localhost:8080/api/health
```

Response:
```json
{"groups":["liveness","readiness"],"status":"UP"}
```

#### Encrypt PII
```bash
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiData": "user@example.com",
    "requestId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

Response:
```json
{
  "tokenId": "550e8400-e29b-41d4-a716-446655440001",
  "encryptedData": "encrypted_base64_string...",
  "encryptedAt": "2026-07-16T20:22:35Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Decrypt Token
```bash
curl -X POST http://localhost:8080/api/v1/decrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiData": "encrypted_base64_string...",
    "requestId": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

#### View Metrics
```bash
curl http://localhost:8080/api/prometheus | grep pii_vault
```

#### All Actuator Endpoints
```bash
curl http://localhost:8080/api/metrics
```

### Full API Documentation
See [API.md](API.md) for complete endpoint documentation including:
- Request/response formats
- All parameters and fields
- Error codes
- Batch operations (Phase 2)
- Admin endpoints (Phase 2)

---

## Workflow 4: Debugging
```bash
# Terminal 1: Start infrastructure
podman-compose up

# Terminal 2: Run app with debug port open
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Terminal 3 (in IDE): Attach debugger to localhost:5005
# Set breakpoints and debug!
```

---

## Troubleshooting

### App Won't Start
```bash
# Check if app is already running on port 8080
lsof -i :8080

# Kill existing process
kill -9 <PID>

# Restart app
mvn spring-boot:run
```

### PostgreSQL Connection Refused
```bash
# Check PostgreSQL is running
podman-compose ps postgres

# View PostgreSQL logs
podman-compose logs postgres

# Restart PostgreSQL
podman-compose restart postgres

# Wait a few seconds, then restart app
mvn spring-boot:run
```

### LocalStack KMS Not Working
```bash
# Check LocalStack is running
podman-compose ps localstack

# Check KMS service is available
curl http://localhost:4566/

# View LocalStack logs
podman-compose logs localstack

# Restart LocalStack
podman-compose restart localstack
```

### Metrics Not Appearing in Grafana
```bash
# Check VictoriaMetrics is running
podman-compose ps victoriametrics

# Check app is exporting metrics
curl http://localhost:8080/api/actuator/prometheus | head -20

# Wait 15 seconds (metrics push interval), then refresh Grafana
```

### JAR Not Found
```bash
# Build JAR first
mvn clean package -DskipTests

# Verify JAR exists
ls -lh target/pii-vault-1.0.0.jar

# Then build Docker image
podman build -t pii-vault:latest .
```

---

## Essential Commands Reference

| Task | Command |
|------|---------|
| Start infrastructure | `podman-compose up -d` |
| Run app (Maven) | `mvn spring-boot:run` |
| Build JAR | `mvn clean package -DskipTests` |
| Run tests | `mvn test` or `mvn verify` |
| Check app health | `curl http://localhost:8080/api/actuator/health` |
| View app logs | `podman-compose logs -f pii-vault` |
| View all logs | `podman-compose logs -f` |
| Access Grafana | `http://localhost:3000` (admin/admin) |
| Connect to PostgreSQL | `podman exec -it pii-vault-postgres psql -U vault_user -d pii_vault` |
| Stop all services | `podman-compose stop` |
| Clean up everything | `podman-compose down -v` |

---

## Performance Tips

### Faster Builds
- Use `-DskipTests` when you're confident in changes
- Use `mvn compile` to just check for errors (no JAR)
- Keep terminal window open and use up arrow to re-run commands

### Faster Restarts
- Terminal history: `history` to see recent commands
- Alias: `alias mvnrun='mvn spring-boot:run'` in your shell

### Monitor Performance
- Watch app startup time in logs (should be < 3 seconds)
- Check Grafana dashboards for encryption latency (should be < 50ms p95)
- Monitor PostgreSQL slow queries:
  ```sql
  SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;
  ```

---

## Next Steps

- Read [GETTING_STARTED.md](GETTING_STARTED.md) for architecture overview
- Read [CONTAINER_DEPENDENCIES.md](CONTAINER_DEPENDENCIES.md) for service details
- Read [DOCKER_SETUP.md](DOCKER_SETUP.md) for production-like setup
- Check [CLAUDE.md](../CLAUDE.md) for project standards and conventions

---

**Questions?** Check the troubleshooting section above or review container logs with `podman-compose logs -f`.

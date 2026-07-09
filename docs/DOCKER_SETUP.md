# PII Vault - Podman Development Setup Guide

This guide covers setting up and running PII Vault using Podman for containerized development.

## Prerequisites

### Install Podman

**macOS:**
```bash
brew install podman
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install podman
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install podman
```

### Install Podman Compose

```bash
pip install podman-compose
```

Or install via package manager:

**macOS:**
```bash
brew install podman-compose
```

**Linux:**
```bash
sudo dnf install podman-compose  # Fedora
sudo apt install podman-compose  # Ubuntu/Debian
```

## Quick Start

### 1. Start Services with Podman Compose

```bash
# Navigate to project root
cd /path/to/pii-vault

# Start all services (LocalStack + PostgreSQL + PII Vault)
podman-compose up -d

# View logs
podman-compose logs -f pii-vault
```

### 2. Verify Services are Running

```bash
# List running containers
podman-compose ps

# Expected output:
# CONTAINER ID  IMAGE                          COMMAND              CREATED      STATUS      PORTS                 NAMES
# xxxxx         localstack/localstack:latest  localstack start     2 mins ago   Up 1 min    0.0.0.0:4566->4566   pii-vault-localstack
# xxxxx         postgres:16-alpine            postgres             2 mins ago   Up 2 mins   0.0.0.0:5432->5432   pii-vault-postgres
# xxxxx         pii-vault:latest              java -jar pii-...    2 mins ago   Up 1 min    0.0.0.0:8080->8080   pii-vault-app
```

### 3. Access PII Vault API

```bash
# Health check
curl http://localhost:8080/api/health

# Response:
# {"status":"UP"}
```

### 4. Access LocalStack (AWS Services Emulation)

```bash
# Check LocalStack KMS service
curl http://localhost:4566/

# List KMS keys
aws kms list-keys --endpoint-url http://localhost:4566

# Create a KMS key
aws kms create-key --endpoint-url http://localhost:4566 \
  --description "PII Vault development key"

# AWS Credentials (LocalStack development defaults)
# Access Key: test
# Secret Key: test
# Region: us-east-1
# Endpoint: http://localhost:4566
```

### 5. Access PostgreSQL

```bash
# Connect via psql
psql -h localhost -U vault_user -d pii_vault -c "SELECT version();"

# Password: vault_password_dev

# Or use pgAdmin/DBeaver for GUI access
# Host: localhost
# Port: 5432
# Database: pii_vault
# Username: vault_user
# Password: vault_password_dev
```

## Development Workflow

### Build and Run

```bash
# Build the Docker image
podman-compose build

# Start all services
podman-compose up -d

# View logs in real-time
podman-compose logs -f pii-vault

# Stop services
podman-compose down

# Stop and remove volumes (careful!)
podman-compose down -v
```

### Rebuild After Code Changes

```bash
# Stop services
podman-compose down

# Rebuild image with latest code
podman-compose build --no-cache

# Start services
podman-compose up -d
```

### View Logs

```bash
# All services
podman-compose logs

# Specific service
podman-compose logs pii-vault
podman-compose logs postgres

# Follow logs in real-time
podman-compose logs -f pii-vault

# Last N lines
podman-compose logs --tail 100 pii-vault
```

## LocalStack Integration

LocalStack provides local AWS service emulation for development, including KMS (Key Management Service) needed for PII Vault's key management.

### LocalStack Services

The `podman-compose.yml` includes LocalStack with the following services:

```yaml
SERVICES: kms,cloudtrail,logs
```

- **KMS**: Key Management Service for encryption key management
- **CloudTrail**: CloudTrail logging for audit and compliance
- **Logs**: CloudWatch Logs for application logging

### Accessing LocalStack

**Endpoint URL**: `http://localhost:4566`

**Default Credentials**:
- Access Key ID: `test`
- Secret Access Key: `test`
- Region: `us-east-1`

### Creating KMS Keys

```bash
# List existing keys
aws kms list-keys --endpoint-url http://localhost:4566

# Create a new KMS key
aws kms create-key \
  --endpoint-url http://localhost:4566 \
  --description "PII Vault Development Key"

# Create an alias for the key (easier to reference)
aws kms create-alias \
  --alias-name alias/pii-vault-dev-key \
  --target-key-id <key-id> \
  --endpoint-url http://localhost:4566

# Encrypt data with KMS key
aws kms encrypt \
  --key-id alias/pii-vault-dev-key \
  --plaintext "sensitive-data" \
  --endpoint-url http://localhost:4566

# Decrypt encrypted data
aws kms decrypt \
  --ciphertext-blob <base64-encrypted-data> \
  --endpoint-url http://localhost:4566
```

### Environment Variables for LocalStack

PII Vault automatically connects to LocalStack when running via podman-compose:

```yaml
AWS_REGION: us-east-1
AWS_ACCESS_KEY_ID: test
AWS_SECRET_ACCESS_KEY: test
AWS_ENDPOINT_OVERRIDE_KMS: http://localstack:4566
SPRING_CLOUD_AWS_KMS_ENDPOINT: http://localstack:4566
```

### LocalStack Persistence

LocalStack data is persisted in a Docker volume (`localstack_data`), so your KMS keys and CloudTrail logs survive container restarts:

```bash
# View LocalStack data volume
podman volume inspect pii_vault_localstack_data

# Clear LocalStack data (starts fresh)
podman-compose down -v
```

### Health Checks

LocalStack includes a health check that verifies KMS service is available:

```bash
# Manual health check
podman-compose exec localstack awslocal kms list-keys

# View LocalStack logs
podman-compose logs localstack
```

### Troubleshooting LocalStack

```bash
# Check LocalStack is running
curl http://localhost:4566/

# View detailed logs
podman-compose logs localstack

# Restart LocalStack
podman-compose restart localstack

# Force rebuild LocalStack
podman-compose down
podman-compose build --no-cache
podman-compose up -d localstack
```

## Docker Image Details

### Multi-Stage Build

The Dockerfile uses a multi-stage build for optimal image size:

**Stage 1: Builder**
- Uses `maven:3.9-eclipse-temurin-25` image
- Downloads dependencies
- Compiles code
- Packages JAR

**Stage 2: Runtime**
- Uses `eclipse-temurin:25-jdk-noble` image (smaller base)
- Copies only the built JAR
- Configures health checks
- Runs the application

### Image Layers

```
docker.io/library/eclipse-temurin:25-jdk-noble
├── OS: Ubuntu Noble
├── Java: OpenJDK 25 (latest LTS)
├── Size: ~400MB
└── PII Vault JAR: 59MB
```

### Health Checks

The container includes a health check that runs every 30 seconds:

```bash
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3
```

- **Interval**: Check every 30 seconds
- **Timeout**: Each check has 10-second timeout
- **Start Period**: 40-second grace period before first check
- **Retries**: Mark unhealthy after 3 failures

## Configuration

### Environment Variables

The `podman-compose.yml` sets development defaults:

```yaml
SPRING_PROFILES_ACTIVE: dev
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pii_vault
SPRING_DATASOURCE_USERNAME: vault_user
SPRING_DATASOURCE_PASSWORD: vault_password_dev
SPRING_JPA_HIBERNATE_DDL_AUTO: update
LOGGING_LEVEL_COM_PII: DEBUG
```

### Custom Configuration

Create `.env` file in project root:

```bash
# .env
POSTGRES_PASSWORD=your_secure_password
SPRING_DATASOURCE_PASSWORD=your_secure_password
LOGGING_LEVEL_COM_PII=TRACE
```

Then update `podman-compose.yml` to use variables:

```yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
podman-compose logs pii-vault

# Rebuild image
podman-compose build --no-cache

# Start with verbose logging
podman-compose up pii-vault --force-recreate
```

### Database Connection Issues

```bash
# Check if PostgreSQL is running
podman-compose ps postgres

# Test connection
podman-compose exec postgres psql -U vault_user -d pii_vault -c "SELECT 1;"

# View PostgreSQL logs
podman-compose logs postgres
```

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in podman-compose.yml
# ports:
#   - "8888:8080"  # Use 8888 instead
```

### Clean Up Everything

```bash
# Stop and remove containers
podman-compose down

# Remove volumes (PostgreSQL data)
podman-compose down -v

# Remove dangling images
podman image prune

# Full cleanup
podman system prune -a
```

## Testing the API

### Encrypt PII Data

```bash
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiData": "{\"email\": \"user@example.com\"}",
    "requestId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### Health Check

```bash
curl http://localhost:8080/api/health
```

## Performance Tips

### Limit Resource Usage

Edit `podman-compose.yml` to add resource limits:

```yaml
services:
  pii-vault:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### Use Named Volumes

Volumes persist data between container restarts:

```bash
# Check volume usage
podman volume ls

# Inspect volume
podman volume inspect pii_vault_postgres_data

# Backup volume
podman run --rm -v pii_vault_postgres_data:/data -v $(pwd)/backups:/backup \
  alpine tar czf /backup/postgres-backup.tar.gz /data
```

## Development vs Production

### Development (Current Setup)

- PostgreSQL with persistent volume
- Automatic schema creation (`DDL=update`)
- Debug logging enabled (`DEBUG` level)
- H2 console available
- Loose security constraints

### Production

Create `podman-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: pii_vault
      POSTGRES_USER: vault_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}  # Use secrets!
    volumes:
      - postgres_data_prod:/var/lib/postgresql/data
    networks:
      - pii-vault-network

  pii-vault:
    image: pii-vault:1.0.0
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_PASSWORD: ${DATASOURCE_PASSWORD}
      LOGGING_LEVEL_COM_PII: INFO
    networks:
      - pii-vault-network
    restart: always

volumes:
  postgres_data_prod:
    driver: local

networks:
  pii-vault-network:
    driver: bridge
```

Run with secrets:

```bash
podman-compose -f podman-compose.prod.yml up -d
```

## Next Steps

1. **Local Development**: Use `podman-compose up -d` for quick development
2. **Database Access**: Connect PostgreSQL tools to `localhost:5432`
3. **API Testing**: Use Postman, curl, or REST Client for testing
4. **Integration Tests**: Add test containers using Testcontainers
5. **CI/CD**: Use Podman for GitHub Actions workflows

## Resources

- [Podman Documentation](https://podman.io/)
- [Podman Compose](https://github.com/containers/podman-compose)
- [Spring Boot with Docker](https://spring.io/guides/topicals/spring-boot-docker)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)

---

**Last Updated**: 2026-07-08

# PII Vault

A secure Spring Boot application for managing and encrypting Personally Identifiable Information (PII).

## Overview

PII Vault is an enterprise-grade Java Spring Boot 4.0.0 application designed as a specialized encryption/decryption service for Personally Identifiable Information (PII). It provides token-based architecture with double encryption (AES-256-GCM + AWS KMS), immutable entry design, and PCI DSS v4 compliance.

## Features

- **Token-Based Architecture**: Encrypt PII → receive opaque tokens; decrypt tokens → receive PII
- **Double Encryption**: AES-256-GCM at application layer + AWS KMS key management
- **Immutable Design**: All PII entries are immutable and cryptographically bound to token_id
- **Soft-Delete Capability**: Soft-delete tokens with admin recovery option
- **Event Streaming**: Stream token lifecycle events via CDC for data lake replication
- **Batch Operations**: Support for multiple PII entries with UUID v7 correlation
- **JSON Blob Storage**: All PII types stored as single encrypted JSON blob (83% KMS cost reduction)
- **PCI DSS v4 Compliance**: Double encryption, audit logging, immutability enforcement
- **Spring Security**: Built-in authentication and authorization framework
- **REST API**: Simple, intuitive endpoints for integration
- **Validation**: Input validation using Jakarta Bean Validation

## Technology Stack

| Component | Version | Details |
|-----------|---------|---------|
| **Java** | 25 LTS | Latest LTS with Records support |
| **Spring Boot** | 4.0.0 | Latest framework version |
| **Spring Framework** | 7.0.0 | Latest framework release |
| **Maven** | 3.6+ | Build and dependency management |
| **Records** | Java 21+ | Immutable data carriers (replaces Lombok) |
| **JPA/Hibernate** | Latest | Robust ORM for database operations |
| **PostgreSQL** | 18+ | Production database with latest features |
| **H2 Database** | Latest | Development database |
| **Spring Security** | Latest | Authentication and authorization |
| **Podman/Docker** | Latest | Containerized development environment |
| **AWS KMS** | Latest | Key management and rotation (Phase 2) |

## Project Structure

```
pii-vault/
├── src/
│   ├── main/
│   │   ├── java/com/pii/
│   │   │   ├── PiiVaultApplication.java
│   │   │   ├── controller/
│   │   │   │   └── PiiVaultController.java
│   │   │   ├── service/
│   │   │   │   ├── PiiVaultService.java
│   │   │   │   └── EncryptionService.java
│   │   │   ├── model/
│   │   │   │   └── PiiData.java
│   │   │   ├── repository/
│   │   │   │   └── PiiDataRepository.java
│   │   │   └── dto/
│   │   │       ├── PiiDataRequest.java
│   │   │       └── PiiDataResponse.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/pii/
│           └── PiiVaultApplicationTests.java
└── pom.xml
```

## Getting Started

### Option 1: Local Development (Recommended - with Podman)

#### Prerequisites
- Java 25 or higher
- Maven 3.6+
- Podman (see [DOCKER_SETUP.md](./docs/DOCKER_SETUP.md))

#### Quick Start
```bash
# Clone and navigate to project
git clone https://github.com/dawgone123/pii-vault.git
cd pii-vault

# Start with Podman Compose (PostgreSQL + PII Vault)
podman-compose up -d

# Verify services running
podman-compose ps

# View logs
podman-compose logs -f pii-vault
```

### Option 2: Local Java Development

#### Prerequisites
- Java 25 or higher
- Maven 3.6+
- PostgreSQL 16+ (for production setup)

#### Build and Run
```bash
# Clone and navigate to project
git clone https://github.com/dawgone123/pii-vault.git
cd pii-vault

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080` with context path `/api`.

## API Endpoints

### Health Check
```bash
curl http://localhost:8080/api/health
# Response: {"status":"UP"}
```

### Encrypt PII Data
```bash
curl -X POST http://localhost:8080/api/v1/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiData": "{\"email\": \"user@example.com\"}",
    "requestId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### Decrypt Token
```bash
curl -X POST http://localhost:8080/api/v1/decrypt \
  -H "Content-Type: application/json" \
  -d '{
    "piiData": "encrypted-token-here",
    "requestId": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

### Batch Encrypt
```bash
curl -X POST http://localhost:8080/api/v1/batch/encrypt \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "550e8400-e29b-41d4-a716-446655440002",
    "entries": [
      {
        "uuid": "550e8400-e29b-41d4-a716-446655440003",
        "piiData": "{\"email\": \"user1@example.com\"}"
      }
    ]
  }'
```

See [API.md](./docs/API.md) for complete API documentation.

## Configuration

### Environment Variables (Development)
```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pii_vault
SPRING_DATASOURCE_USERNAME=vault_user
SPRING_DATASOURCE_PASSWORD=vault_password_dev
LOGGING_LEVEL_COM_PII=DEBUG
```

### Podman Development
For detailed Podman setup, environment variables, and troubleshooting, see [DOCKER_SETUP.md](./docs/DOCKER_SETUP.md).

## Documentation

- **[docs/PRD.md](./docs/PRD.md)** - Product Requirements Document with complete specifications
- **[docs/API.md](./docs/API.md)** - REST API documentation with examples
- **[docs/KMS_STRATEGY.md](./docs/KMS_STRATEGY.md)** - KMS key management and rotation strategy
- **[docs/GETTING_STARTED.md](./docs/GETTING_STARTED.md)** - Detailed setup guide
- **[docs/DOCKER_SETUP.md](./docs/DOCKER_SETUP.md)** - Podman/Docker containerization guide
- **[docs/INDEX.md](./docs/INDEX.md)** - Documentation navigation hub

## Project Status

**Current Phase**: v1 (MVP)

### Completed (Phase 1)
- ✅ Spring Boot 4.0.0 with Java 25
- ✅ Java Records for immutable data transfer
- ✅ AES-256-GCM encryption
- ✅ Token-based architecture
- ✅ Single/batch encrypt/decrypt operations
- ✅ UUID v7 support
- ✅ Podman containerization with docker-compose
- ✅ PostgreSQL database schema
- ✅ Comprehensive documentation

### Planned (Phase 2)
- [ ] AWS KMS integration for key management
- [ ] Double encryption with KMS
- [ ] Token lifecycle management
- [ ] JWT/OAuth2 authentication
- [ ] Comprehensive audit endpoints
- [ ] Rate limiting and throttling

### Future (Phase 3)
- [ ] PCI DSS v4 certification
- [ ] Multi-region replication
- [ ] Seamless key decommissioning with automatic re-encryption

## Development

### Code Structure
- **Controllers**: REST API endpoints (`controller/`)
- **Services**: Business logic (`service/`)
- **Repositories**: Data access layer (`repository/`)
- **Models**: JPA entities (`model/`)
- **DTOs**: Data Transfer Objects as Java Records (`dto/`)

### Java Records
PII Vault uses Java Records instead of Lombok:
- `PiiDataRequest`: Immutable request object
- `PiiDataResponse`: Immutable response object
- Automatic `equals()`, `hashCode()`, `toString()`
- No need for boilerplate getter/setter methods

### Build and Testing
```bash
# Compile and run tests
mvn clean compile
mvn test

# Build JAR artifact
mvn clean package

# Run with Maven
mvn spring-boot:run

# Run JAR directly
java -jar target/pii-vault-1.0.0.jar
```

## Next Steps

1. **Read Documentation**: Start with [docs/PRD.md](./docs/PRD.md)
2. **Set Up Development**: Follow [docs/GETTING_STARTED.md](./docs/GETTING_STARTED.md)
3. **Containerize**: Use [docs/DOCKER_SETUP.md](./docs/DOCKER_SETUP.md) for Podman
4. **Test API**: Use curl, Postman, or REST Client
5. **Phase 2**: Begin AWS KMS integration

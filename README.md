# PII Vault

A secure Spring Boot application for managing and encrypting Personally Identifiable Information (PII).

## Overview

PII Vault is a Java Spring Boot Maven project designed to safely store, retrieve, and manage sensitive personal information with built-in AES-256 encryption.

## Features

- **Secure Storage**: Encrypts PII data using AES-256 encryption
- **RESTful API**: Easy-to-use REST endpoints for CRUD operations
- **Database Support**: H2 for development, PostgreSQL ready for production
- **Spring Security**: Built-in authentication and authorization
- **JPA/Hibernate**: Robust ORM for database operations
- **Validation**: Input validation using Jakarta Bean Validation

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Maven**: Build tool
- **JPA/Hibernate**: ORM
- **H2 Database**: Development database
- **PostgreSQL**: Production database
- **Lombok**: Boilerplate reduction
- **Spring Security**: Authentication and authorization

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

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080` with context path `/api`.

## API Endpoints

### Store PII Data
```
POST /api/v1/pii/store
Content-Type: application/json

{
  "dataType": "email",
  "value": "user@example.com",
  "ownerId": "user123"
}
```

### Retrieve PII Data
```
GET /api/v1/pii/{id}
```

### Get PII by Owner
```
GET /api/v1/pii/owner/{ownerId}
```

### Get PII by Type
```
GET /api/v1/pii/type/{dataType}
```

### Delete PII Data
```
DELETE /api/v1/pii/{id}
```

## Database Access (Development)

H2 Console is available at: `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave blank)

## Next Steps

- Configure PostgreSQL for production
- Implement authentication/authorization
- Add audit logging
- Add API documentation (Swagger/OpenAPI)
- Add comprehensive unit and integration tests

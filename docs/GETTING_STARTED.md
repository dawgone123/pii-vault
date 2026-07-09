# Getting Started with PII Vault

This guide will help you set up and run the PII Vault application in your local development environment.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 21 or higher
  ```bash
  java -version
  ```
  Expected output: `openjdk version "21"` or higher

- **Maven**: Version 3.6 or higher
  ```bash
  mvn -version
  ```

- **Git**: For version control
  ```bash
  git --version
  ```

- **PostgreSQL** (Optional, for production setup): Version 14 or higher

## Project Structure

```
pii-vault/
├── docs/                          # Documentation
├── src/
│   ├── main/
│   │   ├── java/com/pii/
│   │   │   ├── PiiVaultApplication.java
│   │   │   ├── controller/        # REST Controllers
│   │   │   ├── service/           # Business Logic
│   │   │   ├── repository/        # Data Access
│   │   │   ├── model/             # JPA Entities
│   │   │   └── dto/               # Data Transfer Objects
│   │   └── resources/
│   │       └── application.yml    # Configuration
│   └── test/
│       └── java/com/pii/          # Unit Tests
├── pom.xml                        # Maven Configuration
├── README.md                      # Project Overview
└── .gitignore                     # Git Ignore Rules
```

## Installation & Setup

### 1. Clone the Repository

```bash
cd /Users/aj/Code
git clone https://github.com/your-org/pii-vault.git
cd pii-vault
```

### 2. Verify Java Installation

```bash
java -version
```

For Spring Boot 4.0.0, you need Java 21 or higher. If you don't have it, install it:

**macOS (using Homebrew):**
```bash
brew install openjdk@21
```

**Ubuntu/Debian:**
```bash
sudo apt-get install openjdk-21-jdk
```

**Windows:**
Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use [AdoptOpenJDK](https://adoptopenjdk.net/)

### 3. Clean and Build

```bash
# Navigate to project root
cd pii-vault

# Clean previous builds
mvn clean

# Build the project
mvn install
```

This will:
- Download all dependencies
- Compile the Java source code
- Run unit tests
- Package the application as a JAR file

### 4. Run the Application

#### Option A: Using Maven

```bash
mvn spring-boot:run
```

#### Option B: Using the JAR File

```bash
java -jar target/pii-vault-1.0.0.jar
```

The application will start on `http://localhost:8080`

### 5. Verify the Application is Running

In a new terminal, test the health endpoint:

```bash
curl http://localhost:8080/api/v1/pii/health
```

Or open in your browser:
```
http://localhost:8080
```

## Development Workflow

### 1. Creating a New Feature

```bash
# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes
# Write tests
# Commit changes

# Push to remote
git push origin feature/your-feature-name
```

### 2. Building During Development

```bash
# Compile without running tests
mvn clean compile

# Build and run tests
mvn clean test

# Build entire project
mvn clean install
```

### 3. Running Specific Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=PiiVaultApplicationTests

# Run a specific test method
mvn test -Dtest=PiiVaultApplicationTests#contextLoads
```

## Database Configuration

### Development (H2 In-Memory)

The default configuration uses H2 in-memory database. No setup needed!

**H2 Console Access:**
- URL: `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave blank)

### Production (PostgreSQL)

For production deployments, you'll need PostgreSQL:

#### 1. Install PostgreSQL

**macOS:**
```bash
brew install postgresql@14
brew services start postgresql@14
```

**Ubuntu/Debian:**
```bash
sudo apt-get install postgresql-14
sudo service postgresql start
```

#### 2. Create Database and User

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE pii_vault;

# Create user
CREATE USER pii_vault_user WITH PASSWORD 'secure_password';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE pii_vault TO pii_vault_user;

# Exit
\q
```

#### 3. Update Application Configuration

Create `src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pii_vault
    username: pii_vault_user
    password: secure_password
    driverClassName: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

#### 4. Run with Production Profile

```bash
java -jar target/pii-vault-1.0.0.jar --spring.profiles.active=prod
```

## IDE Setup

### IntelliJ IDEA

1. Open IntelliJ IDEA
2. File → Open
3. Select the `pii-vault` directory
4. Choose "Open as Project"
5. Wait for Maven to sync dependencies
6. Mark `src/main/java` as Sources Root
7. Mark `src/test/java` as Test Sources Root

### VS Code

1. Install extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Maven for Java

2. Open the `pii-vault` directory
3. VS Code will auto-detect the Maven project
4. Run/Debug configurations will be available

### Eclipse

1. File → Import → Maven → Existing Maven Projects
2. Browse to `pii-vault` directory
3. Click Finish
4. Wait for project to build

## Troubleshooting

### Issue: Java Version Not Found

**Error:** `JAVA_HOME not found` or `java version 21+ required`

**Solution:**
```bash
# Check installed Java versions
/usr/libexec/java_home -V

# Set JAVA_HOME (macOS example)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Verify
java -version
```

### Issue: Maven Build Fails

**Error:** `mvn: command not found`

**Solution:**
```bash
# macOS with Homebrew
brew install maven

# Ubuntu/Debian
sudo apt-get install maven

# Verify
mvn -version
```

### Issue: Port 8080 Already in Use

**Error:** `Address already in use: bind`

**Solution:**
```bash
# Option 1: Kill process using port 8080
lsof -i :8080
kill -9 <PID>

# Option 2: Use different port
java -jar target/pii-vault-1.0.0.jar --server.port=8081
```

### Issue: H2 Console Not Accessible

**Error:** `404 Not Found` when accessing `/api/h2-console`

**Solution:**
- Ensure application is running: `mvn spring-boot:run`
- Check URL is correct: `http://localhost:8080/api/h2-console`
- Check `application.yml` has H2 console enabled: `h2.console.enabled: true`

### Issue: Test Failures

**Error:** Tests fail during build

**Solution:**
```bash
# Run tests with verbose output
mvn test -X

# Skip tests temporarily (not recommended)
mvn install -DskipTests
```

## Next Steps

1. **Read the API Documentation**: See [API.md](./API.md) for endpoint specifications
2. **Understand the Architecture**: Read [ARCHITECTURE.md](./ARCHITECTURE.md)
3. **Set Up IDE Debugging**: Configure breakpoints and debug configurations
4. **Explore the Code**: Start with `PiiVaultApplication.java` and `PiiVaultController.java`
5. **Run Sample Requests**: Test the API using curl or Postman

## Common Commands Reference

```bash
# Build and run tests
mvn clean install

# Run the application
mvn spring-boot:run

# Build JAR
mvn clean package

# Run JAR
java -jar target/pii-vault-1.0.0.jar

# Clean build artifacts
mvn clean

# Check dependency tree
mvn dependency:tree

# Format code
mvn spotless:apply
```

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Java 21 Features](https://www.oracle.com/java/technologies/javase/jdk21-doc.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

**Last Updated**: 2026-07-07

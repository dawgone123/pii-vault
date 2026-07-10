# Multi-stage build for PII Vault
# Stage 1: Build (Latest Maven with Java 25)
FROM docker.io/library/maven:3.9.9-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Latest Java 25 on Ubuntu)
FROM docker.io/library/eclipse-temurin:25-jdk-noble

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/pii-vault-1.0.0.jar .

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp pii-vault-1.0.0.jar -Dspring.profiles.active=health \
    org.springframework.boot.loader.launch.JarLauncher || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "pii-vault-1.0.0.jar"]

# Default command with dev profile
CMD ["--spring.profiles.active=dev"]

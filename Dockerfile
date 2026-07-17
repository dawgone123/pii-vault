# PII Vault Runtime (Java 25 JRE)
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy pre-built JAR from local build
COPY target/pii-vault-1.0.0.jar .

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "pii-vault-1.0.0.jar"]
CMD ["--spring.profiles.active=dev"]

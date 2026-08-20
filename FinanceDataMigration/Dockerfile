# --- Stage 1: Build Stage ---
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy dependency definition to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Ensure proper permissions
RUN chown -R appuser:appgroup /app
USER appuser

# Expose the server port defined in your properties
EXPOSE 8000

# Environment defaults (Can be overridden at container runtime)
ENV JAVA_OPTS=""

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
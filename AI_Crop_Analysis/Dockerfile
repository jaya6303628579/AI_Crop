# Build stage - Compile the application
FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -q

# Runtime stage - Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/AI_Crop_Analysis/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]

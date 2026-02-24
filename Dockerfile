# Multi-stage build for LLM Conversational Agent

# Stage 1: Builder
FROM sbtscala/scala-sbt:eclipse-temurin-11.0.17_8_1.9.7_3.3.0 AS builder

WORKDIR /app

# Copy project files
COPY build.sbt .
COPY project/ ./project/
COPY src/ ./src/

# Build the application
RUN sbt clean assembly

# Stage 2: Runtime
FROM eclipse-temurin:11.0.17_8-jre-alpine

WORKDIR /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init curl

# Copy the fat JAR from builder
COPY --from=builder /app/target/scala-3.5.0/LLMConversationalAgent-assembly-*.jar ./app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Expose port
EXPOSE 8080

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run the application
CMD ["java", "-Xmx1g", "-Xms512m", "-jar", "app.jar"]

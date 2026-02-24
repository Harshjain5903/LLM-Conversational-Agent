# Multi-stage build for LLM Conversational Agent
# Optimized for production deployment
# Author: Harsh Jain

# Stage 1: Builder
FROM sbtscala/scala-sbt:eclipse-temurin-11.0.17_8_1.9.7_3.3.0 AS builder

WORKDIR /app

# Copy build configuration
COPY build.sbt .
COPY project/ ./project/
COPY .scalafmt.conf .

# Copy source code
COPY src/ ./src/

# Build the application
RUN sbt clean update compile
RUN sbt assembly

# Stage 2: Runtime
FROM eclipse-temurin:11.0.17_8-jre-alpine

# Install runtime dependencies
RUN apk add --no-cache \
    dumb-init \
    curl \
    ca-certificates

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /sbin/nologin app

# Copy the fat JAR from builder
COPY --from=builder /app/target/scala-3.5.0/LLMConversationalAgent-assembly-*.jar ./app.jar

# Create logs directory
RUN mkdir -p /var/log/llm-agent && chown -R app:app /var/log/llm-agent

# Set ownership
RUN chown -R app:app /app

# Switch to non-root user
USER app

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run the application with optimized JVM settings
CMD ["java", \
     "-Xmx1g", \
     "-Xms512m", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     "-XX:+ParallelRefProcEnabled", \
     "-XX:+UnlockDiagnosticVMOptions", \
     "-XX:G1SummarizeRSetStatsPeriod=1", \
     "-jar", "app.jar"]


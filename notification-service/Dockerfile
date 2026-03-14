# ============================================================
# Multi-stage build para o Notification Service
# ============================================================

# Stage 1: Build com Gradle
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .. .
RUN ./gradlew :notification-service:bootJar --no-daemon -x test

# Stage 2: Runtime mínimo com JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=builder /app/notification-service/build/libs/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]

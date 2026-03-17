# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# 1. Copia arquivos de build PRIMEIRO para aproveitar o cache do Docker
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Baixa as dependências e faz cache dessa camada
RUN ./gradlew dependencies --no-daemon || true

# 2. Copia o código-fonte
COPY event-schemas/ event-schemas/
COPY stream-processing-service/ stream-processing-service/

RUN ./gradlew :stream-processing-service:bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="streaming-platform-team"
LABEL service="stream-processing-service"

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

COPY --from=build /workspace/stream-processing-service/build/libs/*-SNAPSHOT.jar app.jar

# Diretório para o state store do Kafka Streams (RocksDB)
RUN mkdir -p /app/state-store && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080
EXPOSE 8081

# Declaração de VOLUME para evitar degradação de I/O no RocksDB
VOLUME ["/app/state-store"]

# Habilita o Generational ZGC e define como JAVA_TOOL_OPTIONS para injeção automática
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# ENTRYPOINT em formato de array para garantir PID 1 e fechamento seguro do State Store
ENTRYPOINT ["java", "-jar", "app.jar"]
# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# 1. Otimização de Cache: Copia apenas os arquivos do Gradle primeiro
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Baixa as dependências para criar uma camada de cache no Docker.
# Isso acelera drasticamente os próximos builds se você mudar apenas o código Java.
RUN ./gradlew dependencies --no-daemon || true

# 2. Copia o código-fonte
COPY event-schemas/ event-schemas/
COPY query-service/ query-service/

# Compila o artefato
RUN ./gradlew :query-service:bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="streaming-platform-team"
LABEL service="query-service"

# Segurança: Execução com usuário sem privilégios de root
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

COPY --from=build /workspace/query-service/build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# Injeção nativa de opções da JVM
# Adicionado -XX:+ZGenerational para suportar alta concorrência de leitura com baixa latência
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# ENTRYPOINT em formato array ("exec form")
# Transfere o PID 1 para o processo Java. Essencial para o Graceful Shutdown funcionar,
# permitindo que o Tomcat conclua as requisições em andamento e libere os pools do Postgres e Redis.
ENTRYPOINT ["java", "-jar", "app.jar"]
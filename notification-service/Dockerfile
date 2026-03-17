# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# 1. Otimização de Cache: Copia apenas os arquivos do Gradle primeiro
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Baixa as dependências e faz o cache dessa camada no Docker.
# Evita baixar o Resilience4j, Spring Kafka e Spring Boot a cada alteração de código.
RUN ./gradlew dependencies --no-daemon || true

# 2. Copia o código-fonte
COPY event-schemas/ event-schemas/
COPY notification-service/ notification-service/

# Compila o artefato
RUN ./gradlew :notification-service:bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="streaming-platform-team"
LABEL service="notification-service"

# Segurança: Execução com usuário sem privilégios de root
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

COPY --from=build /workspace/notification-service/build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# Injeção nativa de opções da JVM
# Adicionado -XX:+ZGenerational para lidar com o alto throughput gerado pelas Virtual Threads
# ao realizar I/O concorrente com APIs de notificação.
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# ENTRYPOINT em formato array ("exec form")
# Transfere o PID 1 para o processo Java. Essencial para que as threads virtuais
# finalizem seus disparos de notificação e o offset do Kafka seja comitado.
ENTRYPOINT ["java", "-jar", "app.jar"]
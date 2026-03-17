# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# 1. Copia apenas os arquivos de configuração do Gradle PRIMEIRO
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Baixa as dependências e faz o cache da camada do Docker
# Isso evita o download repetitivo da internet se o código-fonte mudar
RUN ./gradlew dependencies --no-daemon || true

# 2. Copia os módulos de código-fonte necessários
COPY event-schemas/ event-schemas/
COPY persistence-service/ persistence-service/

# Compila o .jar pulando os testes
RUN ./gradlew :persistence-service:bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="streaming-platform-team"
LABEL service="persistence-service"

# Prática de segurança mantida
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

# Copia o jar resultante
COPY --from=build /workspace/persistence-service/build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# Injeção automática via JAVA_TOOL_OPTIONS
# Adicionado -XX:+ZGenerational para máxima eficiência no Java 21
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# ENTRYPOINT via array nativo do Docker
# Garante que o Java rode no PID 1, repassando o SIGTERM para o Spring Boot
# fechar o pool de conexões (HikariCP) e os consumidores Kafka corretamente.
ENTRYPOINT ["java", "-jar", "app.jar"]
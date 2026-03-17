# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# 1. Copia PRIMEIRO apenas os arquivos necessários para baixar as dependências
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Baixa as dependências do Gradle (Aproveitando o cache de camadas do Docker)
# O comando abaixo falhará ao compilar pois não há código fonte ainda,
# mas fará o download das dependências da internet.
RUN ./gradlew dependencies --no-daemon || true

# 2. Copia o código-fonte APÓS baixar as dependências
COPY event-schemas/ event-schemas/
COPY ingestion-service/ ingestion-service/

# Executa o build final
RUN ./gradlew :ingestion-service:bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="streaming-platform-team"
LABEL service="ingestion-service"

# Criação de usuário não-root (Excelente prática de segurança mantida)
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

# Copia o jar gerado
COPY --from=build /workspace/ingestion-service/build/libs/*-SNAPSHOT.jar app.jar

# Garante as permissões corretas
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
EXPOSE 8081

# Uso do JAVA_TOOL_OPTIONS: A própria JVM reconhece essa variável nativamente.
# Adicionado +ZGenerational para maximizar a performance no Java 21
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"

# Nota sobre o Healthcheck: A imagem base ubuntu do eclipse-temurin contém 'curl'.
# Se no futuro migrarem para a versão 'alpine' (-alpine), será necessário instalar o curl via apk.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# ENTRYPOINT corrigido para usar a sintaxe exec (array), garantindo que o Java assuma o PID 1.
# Não é necessário passar a variável de ambiente explicitamente aqui, o Java a lerá automaticamente.
ENTRYPOINT ["java", "-jar", "app.jar"]
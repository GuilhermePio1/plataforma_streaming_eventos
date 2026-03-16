plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":event-schemas"))

    // Spring Boot Core
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // O Jackson (databind e jsr310) já vem embutido no spring-boot-starter-web!

    // Stream Processing
    implementation(libs.spring.kafka)
    implementation(libs.kafka.streams)

    // Resiliência
    implementation(libs.resilience4j.spring.boot)
    implementation(libs.resilience4j.circuitbreaker)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes de integração
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka.test) // Corrigido
    testImplementation(libs.kafka.streams.test.utils) // Movido para o TOML
}
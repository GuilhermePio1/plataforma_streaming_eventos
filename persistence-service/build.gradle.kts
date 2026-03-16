plugins {
    alias(libs.plugins.spring.boot)
}

// Bloco dependencyManagement removido, pois agora é herdado da raiz!

dependencies {
    implementation(project(":event-schemas"))

    // Spring Boot Core
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    // Banco de Dados e Persistência (PostgreSQL + JPA)
    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql)

    // Mensageria (Consumer)
    implementation(libs.spring.kafka)

    // Resiliência
    implementation(libs.resilience4j.spring.boot)
    implementation(libs.resilience4j.circuitbreaker)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka.test) // Corrigido para a biblioteca de utilitários de teste
}
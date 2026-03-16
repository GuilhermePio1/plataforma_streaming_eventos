plugins {
    alias(libs.plugins.spring.boot)
}

// Bloco dependencyManagement removido (agora herdado da raiz)

dependencies {
    implementation(project(":event-schemas"))

    // Spring Boot Core
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // Mensageria (Consumer)
    implementation(libs.spring.kafka)

    // Resiliência (Essencial para proteger chamadas a APIs de Notificação Externas)
    implementation(libs.resilience4j.spring.boot)
    implementation(libs.resilience4j.circuitbreaker)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes de integração
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka.test) // Corrigido para a biblioteca de utilitários de teste
}
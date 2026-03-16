plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":event-schemas"))

    // Spring Boot Core
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // gRPC Server (Descomente se for implementar o endpoint de alta performance agora)
    // implementation(libs.grpc.server.spring.boot)

    // Mensageria
    implementation(libs.spring.kafka)

    // Resiliência
    implementation(libs.resilience4j.spring.boot)
    implementation(libs.resilience4j.circuitbreaker)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka.test) // <-- Corrigido para utilizar as ferramentas de teste do Kafka
}
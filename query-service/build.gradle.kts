plugins {
    alias(libs.plugins.spring.boot)
}

// Bloco dependencyManagement removido (agora herdado da configuração global na raiz)

dependencies {
    implementation(project(":event-schemas"))

    // Spring Boot Core
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    // Persistência e Cache (PostgreSQL + JPA + Redis)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    runtimeOnly(libs.postgresql)

    // Mensageria (Consumer para manter a materialização de dados atualizada)
    implementation(libs.spring.kafka)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes de Integração
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka.test) // Corrigido para a biblioteca utilitária de testes
}
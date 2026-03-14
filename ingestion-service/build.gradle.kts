plugins {
    alias(libs.plugins.spring.boot)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.springCloud.get()}")
    }
}

dependencies {
    implementation(project(":event-schemas"))

    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    // Kafka Producer
    implementation(libs.spring.kafka)

    // Resiliência
    implementation(libs.resilience4j.spring.boot)
    implementation(libs.resilience4j.circuitbreaker)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes de integração
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka)
}

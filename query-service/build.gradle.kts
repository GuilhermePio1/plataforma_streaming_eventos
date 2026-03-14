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
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)

    // Serialização JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // PostgreSQL
    runtimeOnly(libs.postgresql)

    // Kafka Consumer (para manter materialização de dados)
    implementation(libs.spring.kafka)

    // Observabilidade
    implementation(libs.opentelemetry.api)

    // Testes de integração
    testImplementation(libs.bundles.test.infra)
    testImplementation(libs.spring.kafka)
}

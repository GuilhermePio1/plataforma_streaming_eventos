// O plugin spring.dependency.management já é aplicado no build.gradle.kts raiz

plugins {
    id("org.springframework.cloud.contract") version "4.3.0"
}

// Configuração do plugin para dizer qual é a classe base dos testes gerados
contracts {
    baseClassForTests.set("com.streaming.events.contract.BaseContractTest")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.springCloud.get()}")
    }
}

dependencies {
    // Bean validation para validar os contratos de entrada
    implementation(libs.spring.boot.starter.validation)

    // Serialização JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Adiciona apenas as interfaces puras do Kafka (necessário para o Serializer)
    compileOnly("org.apache.kafka:kafka-clients")

    // =========================================================================
    // Dependências de Teste
    // =========================================================================

    // Kafka clients para testes de serialização/desserialização
    testImplementation("org.apache.kafka:kafka-clients")

    // Spring Cloud Contract (Verifier) para testes de contrato
    testImplementation("org.springframework.cloud:spring-cloud-contract-verifier")
}
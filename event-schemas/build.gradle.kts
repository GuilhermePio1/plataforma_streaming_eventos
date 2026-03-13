// O plugin spring.dependency.management já é aplicado no build.gradle.kts raiz

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
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
}
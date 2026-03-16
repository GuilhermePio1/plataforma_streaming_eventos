import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
    group = "com.streaming"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

// =======================================================================
// Lemos todas as variáveis do TOML aqui no escopo raiz, onde o 'libs' funciona perfeitamente
val projetoJavaVersion = libs.versions.java.get()
val lombokDependency = libs.lombok
val springBootVersion = libs.versions.springBoot.get()
val springCloudVersion = libs.versions.springCloud.get()
// =======================================================================

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    // Usamos as variáveis locais no lugar da chamada direta ao 'libs'
    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(projetoJavaVersion))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")

        add("compileOnly", lombokDependency)
        add("annotationProcessor", lombokDependency)
    }
}
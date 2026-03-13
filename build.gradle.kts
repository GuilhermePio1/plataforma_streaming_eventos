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

val projetoJavaVersion = libs.versions.java.get()
val lombokDependency = libs.lombok

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

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
plugins {
    java
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.poc.training"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            excludeTags("integration")
        }
        systemProperty("testcontainers.ryuk.disabled", "true")
        systemProperty("ryuk.disabled", "true")
        environment("TESTCONTAINERS_RYUK_DISABLED", "true")
        environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "npipe:////./pipe/podman-machine-default")
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(
            classDirectories.files.map { dir ->
                fileTree(dir) {
                    exclude(
                        "**/*Application.class"
                    )
                }
            }
        )
    }

    tasks.withType<JacocoCoverageVerification> {
        classDirectories.setFrom(
            classDirectories.files.map { dir ->
                fileTree(dir) {
                    exclude(
                        "**/*Application.class"
                    )
                }
            }
        )
        violationRules {
            rule {
                element = "BUNDLE"
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = "1.0".toBigDecimal()
                }
            }
        }
    }

    // Integration test task
    tasks.register("integrationTest", Test::class) {
        useJUnitPlatform {
            includeTags("integration")
        }
        description = "Runs integration tests with Testcontainers"
        group = "verification"
        shouldRunAfter("test")
        systemProperty("testcontainers.ryuk.disabled", "true")
        systemProperty("ryuk.disabled", "true")
        environment("TESTCONTAINERS_RYUK_DISABLED", "true")
        environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "npipe:////./pipe/podman-machine-default")
    }

    // Unit test task (excluding integration tests)
    tasks.register("unitTest", Test::class) {
        useJUnitPlatform {
            excludeTags("integration")
        }
        description = "Runs unit tests only"
        group = "verification"
    }
}

// E2E test task at root level
tasks.register("e2eTest", Test::class) {
    useJUnitPlatform()
    description = "Runs E2E tests"
    group = "verification"
    maxParallelForks = 1
    shouldRunAfter("test")
    // @BeforeAll starts Postgres + RabbitMQ + Keycloak + 5 microservices (~11 min total)
    systemProperty("junit.jupiter.execution.timeout.lifecycle.method.default", "1200s")
    // Each individual test method should complete in under 2 minutes
    systemProperty("junit.jupiter.execution.timeout.test.method.default", "120s")
    systemProperty("testcontainers.ryuk.disabled", "true")
    systemProperty("ryuk.disabled", "true")
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "npipe:////./pipe/podman-machine-default")
}

tasks.register("testAll", Test::class) {
    useJUnitPlatform()
    description = "Runs all tests: unit + integration"
    group = "verification"
    shouldRunAfter("test")
}

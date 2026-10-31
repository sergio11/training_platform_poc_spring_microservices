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
        environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "npipe:////./pipe/docker_engine")
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
        dependsOn("test")
        violationRules {
            rule {
                element = "BUNDLE"
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = "1.0".toBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "1.0".toBigDecimal()
                }
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "1.0".toBigDecimal()
                }
                limit {
                    counter = "METHOD"
                    value = "COVEREDRATIO"
                    minimum = "1.0".toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn("jacocoTestCoverageVerification")
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
        environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "npipe:////./pipe/docker_engine")
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

// Convenience alias: the E2E suite lives in the e2e-tests module, where the
// Test task is configured with the container lifecycle and extended timeouts.
tasks.register("e2eTest") {
    group = "verification"
    description = "Runs the full E2E suite defined in :e2e-tests"
    dependsOn(":e2e-tests:test")
}

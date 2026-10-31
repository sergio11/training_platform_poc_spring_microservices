plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":services:training-catalog-service"))
    implementation(project(":services:enrollment-service"))
    implementation(project(":services:certification-service"))
    implementation(project(":services:notification-service"))
    implementation(project(":services:employee-service"))

    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("org.awaitility:awaitility")
}

tasks.test {
    useJUnitPlatform()
    // @BeforeAll starts Postgres + RabbitMQ + Keycloak + 5 microservices (~11 min total)
    systemProperty("junit.jupiter.execution.timeout.lifecycle.method.default", "1200s")
    // Each individual test method should complete in under 2 minutes
    systemProperty("junit.jupiter.execution.timeout.test.method.default", "120s")
    systemProperty("testcontainers.ryuk.disabled", "true")
    maxParallelForks = 1
    forkEvery = 100
}

// This module contains no production classes, so a coverage gate is meaningless.
tasks.withType<JacocoReport> {
    enabled = false
}

tasks.withType<JacocoCoverageVerification> {
    enabled = false
}

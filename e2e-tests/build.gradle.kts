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
    systemProperty("junit.jupiter.execution.timeout.default", "600s")
    systemProperty("testcontainers.ryuk.disabled", "true")
    maxParallelForks = 1
    forkEvery = 100
}

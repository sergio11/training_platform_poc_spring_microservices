package com.poc.training.e2e.base;

import tools.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);

    private static boolean initialized = false;
    private static volatile boolean setupAttempted = false;

    static {
        System.setProperty("testcontainers.ryuk.disabled", "true");
        System.setProperty("ryuk.disabled", "true");
        if (System.getenv("DOCKER_HOST") == null) {
            System.setProperty("docker.host", "npipe:////./pipe/podman-machine-default");
        }
    }

    protected static PostgreSQLContainer<?> postgres;
    protected static org.testcontainers.containers.GenericContainer<?> rabbitmq;
    protected static org.testcontainers.containers.GenericContainer<?> keycloak;

    protected static String catalogUrl;
    protected static String enrollmentUrl;
    protected static String certificationUrl;
    protected static String notificationUrl;
    protected static String employeeUrl;

    protected static WebClient catalogClient;
    protected static WebClient enrollmentClient;
    protected static WebClient certificationClient;
    protected static WebClient notificationClient;
    protected static WebClient employeeClient;

    private static final Map<String, ConfigurableApplicationContext> contexts = new ConcurrentHashMap<>();

    @BeforeAll
    void setUpAll() {
        synchronized (BaseE2ETest.class) {
            if (initialized || setupAttempted) return;
            setupAttempted = true;

            try {
                log.info("=== Starting E2E infrastructure (single shared instance) ===");

                postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine")
                        .asCompatibleSubstituteFor("postgres"))
                        .withDatabaseName("training_platform")
                        .withUsername("test")
                        .withPassword("test")
                        .withStartupTimeout(Duration.ofSeconds(300));
                postgres.start();

                rabbitmq = new org.testcontainers.containers.GenericContainer<>(
                            DockerImageName.parse("rabbitmq:3.13-alpine"))
                            .withExposedPorts(5672)
                            .withStartupTimeout(Duration.ofSeconds(240))
                            .waitingFor(Wait.forListeningPort()
                                .withStartupTimeout(Duration.ofSeconds(240)));
                rabbitmq.start();

                keycloak = new org.testcontainers.containers.GenericContainer<>(
                        DockerImageName.parse("quay.io/keycloak/keycloak:24.0.1"))
                        .withExposedPorts(8080)
                        .withCommand("start-dev", "--import-realm")
                        .withCopyFileToContainer(
                                MountableFile.forClasspathResource("keycloak/realm-export.json"),
                                "/opt/keycloak/data/import/realm-export.json")
                        .withStartupTimeout(Duration.ofSeconds(600))
                        .waitingFor(Wait.forLogMessage(".*Listening on:.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(600)));
                keycloak.start();

                int keycloakPort = keycloak.getMappedPort(8080);
                String keycloakIssuer = "http://localhost:" + keycloakPort + "/realms/training-realm";
                int rabbitmqPort = rabbitmq.getMappedPort(5672);
                String rabbitmqHost = rabbitmq.getHost();

                createDatabases();

                employeeUrl = startService(
                        "com.poc.training.employee.EmployeeServiceApplication",
                        "employee-service", Map.of(
                                "spring.datasource.url", jdbcUrl("training_employee"),
                                "spring.datasource.username", "test",
                                "spring.datasource.password", "test",
                                "spring.rabbitmq.host", rabbitmqHost,
                                "spring.rabbitmq.port", String.valueOf(rabbitmqPort),
                                "spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer,
                                "spring.jpa.hibernate.ddl-auto", "update",
                                "spring.flyway.enabled", "false"));

                catalogUrl = startService(
                        "com.poc.training.catalog.TrainingCatalogServiceApplication",
                        "catalog-service", Map.of(
                                "spring.datasource.url", jdbcUrl("training_catalog"),
                                "spring.datasource.username", "test",
                                "spring.datasource.password", "test",
                                "spring.rabbitmq.host", rabbitmqHost,
                                "spring.rabbitmq.port", String.valueOf(rabbitmqPort),
                                "spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer,
                                "spring.jpa.hibernate.ddl-auto", "update",
                                "spring.flyway.enabled", "false"));

                enrollmentUrl = startService(
                        "com.poc.training.enrollment.EnrollmentServiceApplication",
                        "enrollment-service", Map.of(
                                "spring.datasource.url", jdbcUrl("training_enrollment"),
                                "spring.datasource.username", "test",
                                "spring.datasource.password", "test",
                                "spring.rabbitmq.host", rabbitmqHost,
                                "spring.rabbitmq.port", String.valueOf(rabbitmqPort),
                                "spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer,
                                "spring.jpa.hibernate.ddl-auto", "update",
                                "spring.flyway.enabled", "false",
                                "client.catalog.url", catalogUrl,
                                "client.employee.url", employeeUrl));

                certificationUrl = startService(
                        "com.poc.training.certification.CertificationServiceApplication",
                        "certification-service", Map.of(
                                "spring.datasource.url", jdbcUrl("training_certification"),
                                "spring.datasource.username", "test",
                                "spring.datasource.password", "test",
                                "spring.rabbitmq.host", rabbitmqHost,
                                "spring.rabbitmq.port", String.valueOf(rabbitmqPort),
                                "spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer,
                                "spring.jpa.hibernate.ddl-auto", "update",
                                "spring.flyway.enabled", "false"));

                notificationUrl = startService(
                        "com.poc.training.notification.NotificationServiceApplication",
                        "notification-service", Map.of(
                                "spring.datasource.url", jdbcUrl("training_notification"),
                                "spring.datasource.username", "test",
                                "spring.datasource.password", "test",
                                "spring.rabbitmq.host", rabbitmqHost,
                                "spring.rabbitmq.port", String.valueOf(rabbitmqPort),
                                "spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer,
                                "spring.jpa.hibernate.ddl-auto", "update",
                                "spring.flyway.enabled", "false"));

                catalogClient = WebClient.builder().baseUrl(catalogUrl)
                        .defaultHeader("Content-Type", "application/json").build();
                enrollmentClient = WebClient.builder().baseUrl(enrollmentUrl)
                        .defaultHeader("Content-Type", "application/json").build();
                certificationClient = WebClient.builder().baseUrl(certificationUrl)
                        .defaultHeader("Content-Type", "application/json").build();
                notificationClient = WebClient.builder().baseUrl(notificationUrl)
                        .defaultHeader("Content-Type", "application/json").build();
                employeeClient = WebClient.builder().baseUrl(employeeUrl)
                        .defaultHeader("Content-Type", "application/json").build();

                waitForHealth(catalogUrl);
                waitForHealth(employeeUrl);
                waitForHealth(enrollmentUrl);
                waitForHealth(certificationUrl);
                waitForHealth(notificationUrl);

                log.info("=== All E2E services started ===");
                initialized = true;
            } catch (Exception e) {
                log.error("E2E setup failed, cleaning up containers", e);
                setupAttempted = false;
                stopAll();
                throw e;
            }
        }
    }

    @AfterAll
    void tearDownAll() {
        // Only tear down on the very last test class
    }

    private static void stopAll() {
        contexts.values().forEach(ctx -> {
            try { ctx.close(); } catch (Exception ignored) {}
        });
        contexts.clear();
        if (rabbitmq != null) try { rabbitmq.stop(); } catch (Exception ignored) {}
        if (postgres != null) try { postgres.stop(); } catch (Exception ignored) {}
        if (keycloak != null) try { keycloak.stop(); } catch (Exception ignored) {}
    }

    // ── Service lifecycle ──

    private static String startService(String mainClassName, String name, Map<String, String> props) {
        try {
            log.info("Starting {}...", name);
            Class<?> mainClass = Class.forName(mainClassName);

            Map<String, Object> allProps = new HashMap<>();
            allProps.put("server.port", "0");
            allProps.put("spring.main.banner-mode", "off");
            allProps.put("spring.main.allow-bean-definition-overriding", "true");
            allProps.putAll(props);

            ConfigurableEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("testOverrides", allProps));

            SpringApplicationBuilder builder = new SpringApplicationBuilder(mainClass)
                    .environment(env);
            ConfigurableApplicationContext ctx = builder.run();
            contexts.put(name, ctx);
            String port = ctx.getEnvironment().getProperty("local.server.port");
            log.info("Started {} on port {}", name, port);
            return "http://localhost:" + port;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start " + name, e);
        }
    }

    private static void waitForHealth(String baseUrl) {
        log.info("Waiting for health at {}/actuator/health", baseUrl);
        WebClient hc = WebClient.create();
        Awaitility.await()
                .atMost(90, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    try {
                        String body = hc.get().uri(baseUrl + "/actuator/health")
                                .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(5));
                        assertThat(body).contains("\"UP\"");
                    } catch (Exception e) {
                        throw new AssertionError("Not ready: " + e.getMessage());
                    }
                });
    }

    private static void createDatabases() {
        String url = postgres.getJdbcUrl().replace("training_platform", "postgres");
        try (var conn = java.sql.DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword())) {
            var stmt = conn.createStatement();
            for (String db : new String[]{"training_catalog", "training_enrollment",
                    "training_certification", "training_notification", "training_employee"}) {
                try { stmt.executeUpdate("CREATE DATABASE " + db); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create databases", e);
        }
    }

    private static String jdbcUrl(String dbName) {
        return postgres.getJdbcUrl().replace("training_platform", dbName);
    }

    // ── JWT token helpers ──

    protected static String adminToken() { return getToken("admin", "admin"); }
    protected static String instructorToken() { return getToken("instructor1", "instructor1"); }
    protected static String employeeToken() { return getToken("employee1", "employee1"); }

    protected static String getToken(String username, String password) {
        int kcPort = keycloak.getMappedPort(8080);
        JsonNode resp = WebClient.create().post()
                .uri("http://localhost:" + kcPort + "/realms/training-realm/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=password&client_id=training-catalog-client"
                        + "&client_secret=catalog-secret-2024&username=" + username + "&password=" + password)
                .retrieve().bodyToMono(JsonNode.class).block();
        return resp.get("access_token").asText();
    }

    // ── Entity creation helpers ──

    protected static Long createCourse(String token, String title, int capacity) {
        Map<?, ?> resp = catalogClient.post().uri("/api/v1/courses")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("title", title, "description", "E2E",
                        "instructor", "Instructor", "capacity", capacity))
                .retrieve().bodyToMono(Map.class).block();
        return Long.valueOf(String.valueOf(resp.get("id")));
    }

    protected static Long createEmployee(String token, String empNumber) {
        Map<?, ?> resp = employeeClient.post().uri("/api/v1/employees")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("employeeNumber", empNumber, "fullName", "Emp " + empNumber,
                        "email", empNumber + "@e2e.test", "department", "IT", "position", "Dev"))
                .retrieve().bodyToMono(Map.class).block();
        return Long.valueOf(String.valueOf(resp.get("id")));
    }

    protected static Map<?, ?> createEnrollment(String token, Long employeeId, Long courseId) {
        return enrollmentClient.post().uri("/api/v1/enrollments")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("employeeId", employeeId, "courseId", courseId))
                .retrieve().bodyToMono(Map.class).block();
    }

    protected static void approveEnrollment(String token, Long enrollmentId) {
        enrollmentClient.patch().uri("/api/v1/enrollments/" + enrollmentId + "/approve")
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();
    }

    protected static void completeEnrollment(String token, Long enrollmentId) {
        enrollmentClient.patch().uri("/api/v1/enrollments/" + enrollmentId + "/complete")
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();
    }

    protected static void waitForAsyncProcessing(ThrowingRunnable assertion) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(assertion);
    }

    protected static void waitForEnrolledCount(Long courseId, int expectedCount, String token) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Map<?, ?> course = catalogClient.get()
                            .uri("/api/v1/courses/{id}", courseId)
                            .header("Authorization", "Bearer " + token)
                            .retrieve().bodyToMono(Map.class).block();
                    assertThat(course.get("enrolledCount"))
                            .as("enrolledCount for course %d", courseId)
                            .isEqualTo(expectedCount);
                });
    }
}

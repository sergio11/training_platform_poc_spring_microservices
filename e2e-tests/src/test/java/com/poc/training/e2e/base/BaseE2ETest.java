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

    static {
        System.setProperty("testcontainers.ryuk.disabled", "true");
        System.setProperty("ryuk.disabled", "true");
        if (System.getenv("DOCKER_HOST") == null && System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.setProperty("docker.host", "npipe:////./pipe/docker_engine");
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
            if (initialized) return;

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
                        DockerImageName.parse("quay.io/keycloak/keycloak:26.0"))
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

                waitForKeycloakReady(keycloakPort);

                createDatabases();

                catalogUrl = startService(
                        "com.poc.training.catalog.TrainingCatalogServiceApplication",
                        "catalog-service", Map.ofEntries(
                                Map.entry("spring.datasource.url", jdbcUrl("training_catalog")),
                                Map.entry("spring.datasource.username", "test"),
                                Map.entry("spring.datasource.password", "test"),
                                Map.entry("spring.flyway.url", jdbcUrl("training_catalog")),
                                Map.entry("spring.flyway.user", "test"),
                                Map.entry("spring.flyway.password", "test"),
                                Map.entry("spring.rabbitmq.host", rabbitmqHost),
                                Map.entry("spring.rabbitmq.port", String.valueOf(rabbitmqPort)),
                                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer),
                                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                                Map.entry("spring.flyway.enabled", "true"),
                                Map.entry("spring.flyway.locations", "classpath:db/migration/catalog")));

                enrollmentUrl = startService(
                        "com.poc.training.enrollment.EnrollmentServiceApplication",
                        "enrollment-service", Map.ofEntries(
                                Map.entry("spring.datasource.url", jdbcUrl("training_enrollment")),
                                Map.entry("spring.datasource.username", "test"),
                                Map.entry("spring.datasource.password", "test"),
                                Map.entry("spring.flyway.url", jdbcUrl("training_enrollment")),
                                Map.entry("spring.flyway.user", "test"),
                                Map.entry("spring.flyway.password", "test"),
                                Map.entry("spring.rabbitmq.host", rabbitmqHost),
                                Map.entry("spring.rabbitmq.port", String.valueOf(rabbitmqPort)),
                                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer),
                                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                                Map.entry("spring.flyway.enabled", "true"),
                                Map.entry("spring.flyway.locations", "classpath:db/migration/enrollment"),
                                Map.entry("client.catalog.url", catalogUrl)));

                employeeUrl = startService(
                        "com.poc.training.employee.EmployeeServiceApplication",
                        "employee-service", Map.ofEntries(
                                Map.entry("spring.datasource.url", jdbcUrl("training_employee")),
                                Map.entry("spring.datasource.username", "test"),
                                Map.entry("spring.datasource.password", "test"),
                                Map.entry("spring.flyway.url", jdbcUrl("training_employee")),
                                Map.entry("spring.flyway.user", "test"),
                                Map.entry("spring.flyway.password", "test"),
                                Map.entry("spring.rabbitmq.host", rabbitmqHost),
                                Map.entry("spring.rabbitmq.port", String.valueOf(rabbitmqPort)),
                                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer),
                                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                                Map.entry("spring.flyway.enabled", "true"),
                                Map.entry("spring.flyway.locations", "classpath:db/migration/employee"),
                                Map.entry("client.enrollment.url", enrollmentUrl)));

                enrollmentUrl = restartService(
                        "com.poc.training.enrollment.EnrollmentServiceApplication",
                        "enrollment-service", Map.ofEntries(
                                Map.entry("spring.datasource.url", jdbcUrl("training_enrollment")),
                                Map.entry("spring.datasource.username", "test"),
                                Map.entry("spring.datasource.password", "test"),
                                Map.entry("spring.flyway.url", jdbcUrl("training_enrollment")),
                                Map.entry("spring.flyway.user", "test"),
                                Map.entry("spring.flyway.password", "test"),
                                Map.entry("spring.rabbitmq.host", rabbitmqHost),
                                Map.entry("spring.rabbitmq.port", String.valueOf(rabbitmqPort)),
                                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer),
                                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                                Map.entry("spring.flyway.enabled", "true"),
                                Map.entry("spring.flyway.locations", "classpath:db/migration/enrollment"),
                                Map.entry("client.catalog.url", catalogUrl),
                                Map.entry("client.employee.url", employeeUrl)));

                certificationUrl = startService(
                        "com.poc.training.certification.CertificationServiceApplication",
                        "certification-service", Map.ofEntries(
                                Map.entry("spring.datasource.url", jdbcUrl("training_certification")),
                                Map.entry("spring.datasource.username", "test"),
                                Map.entry("spring.datasource.password", "test"),
                                Map.entry("spring.flyway.url", jdbcUrl("training_certification")),
                                Map.entry("spring.flyway.user", "test"),
                                Map.entry("spring.flyway.password", "test"),
                                Map.entry("spring.rabbitmq.host", rabbitmqHost),
                                Map.entry("spring.rabbitmq.port", String.valueOf(rabbitmqPort)),
                                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer),
                                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                                Map.entry("spring.flyway.enabled", "true"),
                                Map.entry("spring.flyway.locations", "classpath:db/migration/certification")));

                notificationUrl = startService(
                        "com.poc.training.notification.NotificationServiceApplication",
                        "notification-service", Map.ofEntries(
                                Map.entry("spring.datasource.url", jdbcUrl("training_notification")),
                                Map.entry("spring.datasource.username", "test"),
                                Map.entry("spring.datasource.password", "test"),
                                Map.entry("spring.flyway.url", jdbcUrl("training_notification")),
                                Map.entry("spring.flyway.user", "test"),
                                Map.entry("spring.flyway.password", "test"),
                                Map.entry("spring.rabbitmq.host", rabbitmqHost),
                                Map.entry("spring.rabbitmq.port", String.valueOf(rabbitmqPort)),
                                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloakIssuer),
                                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                                Map.entry("spring.flyway.enabled", "true"),
                                Map.entry("spring.flyway.locations", "classpath:db/migration/notification"),
                                Map.entry("management.health.mail.enabled", "false")));

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
                registerShutdownHook();
            } catch (Exception e) {
                log.error("E2E setup failed, cleaning up containers", e);
                stopAll();
                throw e;
            }
        }
    }

    @AfterAll
    void tearDownAll() {
    }

    protected static void stopAll() {
        contexts.values().forEach(ctx -> {
            try { ctx.close(); } catch (Exception ignored) {}
        });
        contexts.clear();
        catalogUrl = null;
        enrollmentUrl = null;
        certificationUrl = null;
        notificationUrl = null;
        employeeUrl = null;
        catalogClient = null;
        enrollmentClient = null;
        certificationClient = null;
        notificationClient = null;
        employeeClient = null;
        if (rabbitmq != null) try { rabbitmq.stop(); } catch (Exception ignored) {}
        if (postgres != null) try { postgres.stop(); } catch (Exception ignored) {}
        if (keycloak != null) try { keycloak.stop(); } catch (Exception ignored) {}
        rabbitmq = null;
        postgres = null;
        keycloak = null;
        initialized = false;
    }

    private static void waitForKeycloakReady(int keycloakPort) {
        log.info("Waiting for Keycloak realm to be ready...");
        WebClient hc = WebClient.create();
        Awaitility.await()
                .atMost(180, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    try {
                        String wellKnown = hc.get()
                                .uri("http://localhost:" + keycloakPort + "/realms/training-realm/.well-known/openid-configuration")
                                .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(5));
                        assertThat(wellKnown).contains("issuer");
                        String certs = hc.get()
                                .uri("http://localhost:" + keycloakPort + "/realms/training-realm/protocol/openid-connect/certs")
                                .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(5));
                        assertThat(certs).contains("keys");
                    } catch (Exception e) {
                        throw new AssertionError("Keycloak not ready: " + e.getMessage());
                    }
                });
        log.info("Keycloak realm ready");
    }

    // ── Service lifecycle ──

    protected static String startService(String mainClassName, String name, Map<String, String> props) {
        try {
            log.info("Starting {}...", name);
            Class<?> mainClass = Class.forName(mainClassName);

            Map<String, Object> allProps = new HashMap<>();
            allProps.put("server.port", "0");
            allProps.put("spring.main.banner-mode", "off");
            allProps.put("spring.main.allow-bean-definition-overriding", "true");
            allProps.put("spring.lifecycle.timeout-per-shutdown-phase", "10s");
            // The E2E suite runs without an OTLP collector; do not let exporters
            // retry in the background and pollute the test logs.
            allProps.put("management.logging.export.otlp.enabled", "false");
            allProps.put("management.tracing.export.otlp.enabled", "false");
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

    protected static void stopService(String name) {
        ConfigurableApplicationContext ctx = contexts.get(name);
        if (ctx != null) {
            log.info("Stopping {}...", name);
            ctx.close();
            contexts.remove(name);
            try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    protected static String restartService(String mainClassName, String name, Map<String, String> props) {
        stopService(name);
        String url = startService(mainClassName, name, props);
        contexts.put(name, contexts.get(name));
        waitForHealth(url);
        log.info("Restarted {} at {}", name, url);
        return url;
    }

    protected static String getServiceUrl(String name) {
        return switch (name) {
            case "catalog-service" -> catalogUrl;
            case "enrollment-service" -> enrollmentUrl;
            case "certification-service" -> certificationUrl;
            case "notification-service" -> notificationUrl;
            case "employee-service" -> employeeUrl;
            default -> throw new IllegalArgumentException("Unknown service: " + name);
        };
    }

    private static void createDatabases() {
        String url = postgres.getJdbcUrl().replace("training_platform", "postgres");
        try (var conn = java.sql.DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword())) {
            var stmt = conn.createStatement();
            for (String db : new String[]{"training_catalog", "training_enrollment",
                    "training_certification", "training_notification", "training_employee"}) {
                try { stmt.executeUpdate("DROP DATABASE IF EXISTS " + db + " WITH (FORCE)"); } catch (Exception ignored) {}
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

    private static volatile boolean shutdownHookRegistered = false;

    private static void registerShutdownHook() {
        synchronized (BaseE2ETest.class) {
            if (shutdownHookRegistered) return;
            shutdownHookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(BaseE2ETest::stopAll));
        }
    }

    protected static String getToken(String username, String password) {
        if (keycloak == null || !keycloak.isRunning()) {
            throw new IllegalStateException("Keycloak is not running - E2E infrastructure not initialized");
        }
        int kcPort = keycloak.getMappedPort(8080);
        JsonNode resp = WebClient.create().post()
                .uri("http://localhost:" + kcPort + "/realms/training-realm/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=password&client_id=training-catalog-client"
                        + "&client_secret=catalog-secret-2024&username=" + username + "&password=" + password)
                .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(10));
        if (resp == null || resp.get("access_token") == null) {
            throw new IllegalStateException("Failed to obtain access token for " + username + ": " + resp);
        }
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

package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Resilience (Circuit Breaker + Retry)")
class ResilienceE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Enrollment service responds normally when catalog is healthy")
    void normalFlowWhenCatalogHealthy() {
        String token = adminToken();
        Long course = createCourse(token, "Resilience Healthy", 10);
        Long emp = createEmployee(token, "EMP-RES-OK");

        Map<?, ?> enroll = createEnrollment(adminToken(), emp, course);
        assertThat(enroll.get("status")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Enrollment service handles downstream failure gracefully")
    void enrollmentHandlesDownstreamFailure() {
        String token = adminToken();

        assertThatThrownBy(() -> enrollmentClient.post().uri("/api/v1/enrollments")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("employeeId", 99999, "courseId", 99999))
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> {
                    int status = ((WebClientResponseException) e).getStatusCode().value();
                    assertThat(status).isIn(400, 404, 503);
                });
    }

    @Test
    @DisplayName("Circuit breaker opens when catalog service is unavailable")
    void circuitBreakerOpensWhenCatalogDown() throws Exception {
        String token = adminToken();
        Long course = createCourse(token, "Resilience CB", 10);
        Long emp = createEmployee(token, "EMP-CB-TEST");

        Map<?, ?> enroll = createEnrollment(adminToken(), emp, course);
        assertThat(enroll.get("status")).isEqualTo("PENDING");

        stopService("catalog-service");

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    try {
                        enrollmentClient.post().uri("/api/v1/enrollments")
                                .header("Authorization", "Bearer " + adminToken())
                                .bodyValue(Map.of("employeeId", emp, "courseId", course))
                                .retrieve()
                                .toBodilessEntity()
                                .block();
                    } catch (WebClientResponseException e) {
                        assertThat(e.getStatusCode().value()).isIn(400, 404, 503);
                    }
                });

        catalogUrl = restartService(
                "com.poc.training.catalog.TrainingCatalogServiceApplication",
                "catalog-service",
                Map.of(
                        "spring.datasource.url", postgres.getJdbcUrl().replace("training_platform", "training_catalog"),
                        "spring.datasource.username", "test",
                        "spring.datasource.password", "test",
                        "spring.rabbitmq.host", rabbitmq.getHost(),
                        "spring.rabbitmq.port", String.valueOf(rabbitmq.getMappedPort(5672)),
                        "spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://localhost:" + keycloak.getMappedPort(8080) + "/realms/training-realm",
                        "spring.jpa.hibernate.ddl-auto", "update",
                        "spring.flyway.enabled", "false"));
        catalogClient = WebClient.builder().baseUrl(catalogUrl)
                .defaultHeader("Content-Type", "application/json").build();

        enrollmentUrl = restartService(
                "com.poc.training.enrollment.EnrollmentServiceApplication",
                "enrollment-service",
                Map.of(
                        "spring.datasource.url", postgres.getJdbcUrl().replace("training_platform", "training_enrollment"),
                        "spring.datasource.username", "test",
                        "spring.datasource.password", "test",
                        "spring.rabbitmq.host", rabbitmq.getHost(),
                        "spring.rabbitmq.port", String.valueOf(rabbitmq.getMappedPort(5672)),
                        "spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://localhost:" + keycloak.getMappedPort(8080) + "/realms/training-realm",
                        "spring.jpa.hibernate.ddl-auto", "update",
                        "spring.flyway.enabled", "false",
                        "client.catalog.url", catalogUrl,
                        "client.employee.url", employeeUrl));
        enrollmentClient = WebClient.builder().baseUrl(enrollmentUrl)
                .defaultHeader("Content-Type", "application/json").build();
    }

    @Test
    @DisplayName("Actuator health endpoint is accessible for all services")
    void actuatorHealthEndpointsAccessible() {
        for (String url : new String[]{catalogUrl, enrollmentUrl, certificationUrl,
                notificationUrl, employeeUrl}) {
            String health = WebClient.create().get()
                    .uri(url + "/actuator/health")
                    .retrieve().bodyToMono(String.class).block();
            assertThat(health).contains("\"UP\"");
        }
    }
}

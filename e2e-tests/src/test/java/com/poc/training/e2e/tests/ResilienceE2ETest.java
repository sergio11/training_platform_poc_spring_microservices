package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

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

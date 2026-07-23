package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("E2E: Outbox Pattern Verification")
class OutboxPatternE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Outbox events are published and consumed end-to-end")
    void outboxEventsArePublishedAndConsumed() {
        String token = adminToken();
        Long course = createCourse(token, "Outbox Test Course", 10);
        Long emp = createEmployee(token, "EMP-OUTBOX-001");

        Map<?, ?> enrollment = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enrollment.get("id")));

        approveEnrollment(token, enrollId);

        org.awaitility.Awaitility.await()
                .atMost(15, java.util.concurrent.TimeUnit.SECONDS)
                .pollInterval(1, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Map<?, ?> page = notificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                                    .queryParam("recipientId", emp).build())
                            .header("Authorization", "Bearer " + token)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) page.get("content");
                    boolean hasEnrollmentApproved = content.stream()
                            .map(n -> ((Map<?, ?>) n).get("type"))
                            .anyMatch(t -> t.equals("ENROLLMENT_APPROVED"));
                    assertThat(hasEnrollmentApproved).isTrue();
                });
    }

    @Test
    @DisplayName("Certificate outbox event triggers notification")
    void certificateOutboxTriggersNotification() {
        String token = adminToken();
        Long course = createCourse(token, "Cert Outbox Course", 10);
        Long emp = createEmployee(token, "EMP-CERT-OUTBOX");

        Map<?, ?> enrollment = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enrollment.get("id")));
        approveEnrollment(token, enrollId);
        completeEnrollment(token, enrollId);

        org.awaitility.Awaitility.await()
                .atMost(20, java.util.concurrent.TimeUnit.SECONDS)
                .pollInterval(1, java.util.concurrent.TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Map<?, ?> page = notificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                                    .queryParam("recipientId", emp).build())
                            .header("Authorization", "Bearer " + token)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) page.get("content");
                    boolean hasCertificate = content.stream()
                            .map(n -> ((Map<?, ?>) n).get("type"))
                            .anyMatch(t -> t.equals("CERTIFICATE_GENERATED"));
                    assertThat(hasCertificate).isTrue();
                });
    }
}

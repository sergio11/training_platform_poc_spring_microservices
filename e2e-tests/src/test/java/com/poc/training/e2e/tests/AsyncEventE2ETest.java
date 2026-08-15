package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Asynchronous Events via RabbitMQ")
class AsyncEventE2ETest extends BaseE2ETest {

    @Test
    @Order(1)
    @DisplayName("Enrollment creation triggers ENROLLMENT_CREATED notification")
    void enrollmentCreatedTriggersNotification() {
        String token = adminToken();
        Long course = createCourse(token, "Async Notif Course", 20);
        Long emp = createEmployee(token, "EMP-ASYNC-001");
        Map<?, ?> enroll = createEnrollment(token, emp, course);

        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String currentToken = adminToken();
                    Map<?, ?> page = notificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                                    .queryParam("recipientId", emp).build())
                            .header("Authorization", "Bearer " + currentToken)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) page.get("content");
                    assertThat(content).isNotEmpty();

                    boolean found = content.stream()
                            .map(n -> ((Map<?, ?>) n).get("type"))
                            .anyMatch(t -> t.equals("ENROLLMENT_CREATED"));
                    assertThat(found).isTrue();
                });
    }

    @Test
    @Order(2)
    @DisplayName("Enrollment approval triggers ENROLLMENT_APPROVED notification")
    void enrollmentApprovedTriggersNotification() {
        String token = adminToken();
        Long course = createCourse(token, "Async Approve Course", 10);
        Long emp = createEmployee(token, "EMP-ASYNC-002");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));
        approveEnrollment(instructorToken(), enrollId);

        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String currentToken = adminToken();
                    Map<?, ?> page = notificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                                    .queryParam("recipientId", emp).build())
                            .header("Authorization", "Bearer " + currentToken)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) page.get("content");
                    boolean found = content.stream()
                            .map(n -> ((Map<?, ?>) n).get("type"))
                            .anyMatch(t -> t.equals("ENROLLMENT_APPROVED"));
                    assertThat(found).isTrue();
                });
    }

    @Test
    @Order(3)
    @DisplayName("Course completion triggers COURSE_COMPLETED notification")
    void courseCompletedTriggersNotification() {
        String token = adminToken();
        Long course = createCourse(token, "Complete Notif Course", 10);
        Long emp = createEmployee(token, "EMP-COMP-NOTIF");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));
        approveEnrollment(token, enrollId);
        completeEnrollment(token, enrollId);

        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String currentToken = adminToken();
                    Map<?, ?> page = notificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                                    .queryParam("recipientId", emp).build())
                            .header("Authorization", "Bearer " + currentToken)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) page.get("content");
                    boolean found = content.stream()
                            .map(n -> ((Map<?, ?>) n).get("type"))
                            .anyMatch(t -> t.equals("COURSE_COMPLETED"));
                    assertThat(found).isTrue();
                });
    }

    @Test
    @Order(4)
    @DisplayName("Course completion triggers certificate generation")
    void courseCompletedTriggersCertificate() {
        String token = adminToken();
        Long course = createCourse(token, "Cert Gen Course", 10);
        Long emp = createEmployee(token, "EMP-CERT-GEN");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));
        approveEnrollment(token, enrollId);

        completeEnrollment(token, enrollId);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String currentToken = adminToken();
                    Map<?, ?> certPage = certificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/certificates")
                                    .queryParam("employeeId", emp).build())
                            .header("Authorization", "Bearer " + currentToken)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) certPage.get("content");
                    assertThat(content).isNotEmpty();
                    Map<?, ?> cert = (Map<?, ?>) content.get(0);
                    assertThat(cert.get("certificateNumber")).asString().startsWith("CERT-");
                });
    }

    @Test
    @Order(5)
    @DisplayName("Certificate generation triggers CERTIFICATE_GENERATED notification")
    void certificateGeneratedTriggersNotification() {
        String token = adminToken();
        Long course = createCourse(token, "Cert Notif Course", 10);
        Long emp = createEmployee(token, "EMP-CERT-NOTIF");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));
        approveEnrollment(token, enrollId);
        completeEnrollment(token, enrollId);

        Awaitility.await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String currentToken = adminToken();
                    Map<?, ?> page = notificationClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                                    .queryParam("recipientId", emp).build())
                            .header("Authorization", "Bearer " + currentToken)
                            .retrieve().bodyToMono(Map.class).block();

                    var content = (java.util.List<?>) page.get("content");
                    boolean found = content.stream()
                            .map(n -> ((Map<?, ?>) n).get("type"))
                            .anyMatch(t -> t.equals("CERTIFICATE_GENERATED"));
                    assertThat(found).isTrue();
                });
    }

    @Test
    @Order(6)
    @DisplayName("Notifications are idempotent (no duplicates)")
    void notificationsAreIdempotent() {
        String token = adminToken();
        Long course = createCourse(token, "Idempotent Notif", 10);
        Long emp = createEmployee(token, "EMP-IDEMP");
        Map<?, ?> enroll = createEnrollment(token, emp, course);

        waitForAsyncProcessing(() -> {
            String currentToken = adminToken();
            Map<?, ?> page = notificationClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                            .queryParam("recipientId", emp).build())
                    .header("Authorization", "Bearer " + currentToken)
                    .retrieve().bodyToMono(Map.class).block();

            var content = (java.util.List<?>) page.get("content");
            long enrollmentCreatedCount = content.stream()
                    .map(n -> ((Map<?, ?>) n).get("type"))
                    .filter(t -> t.equals("ENROLLMENT_CREATED"))
                    .count();
            assertThat(enrollmentCreatedCount).isEqualTo(1);
        });
    }

    @Test
    @Order(7)
    @DisplayName("Certificates are idempotent (no duplicates)")
    void certificatesAreIdempotent() {
        String token = adminToken();
        Long course = createCourse(token, "Idempotent Cert", 10);
        Long emp = createEmployee(token, "EMP-IDEMP-CERT");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));
        approveEnrollment(token, enrollId);
        completeEnrollment(token, enrollId);

        waitForAsyncProcessing(() -> {
            String currentToken = adminToken();
            Map<?, ?> certPage = certificationClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/certificates")
                            .queryParam("employeeId", emp).build())
                    .header("Authorization", "Bearer " + currentToken)
                    .retrieve().bodyToMono(Map.class).block();

            var content = (java.util.List<?>) certPage.get("content");
            assertThat(content).hasSize(1);
        });
    }

    @Test
    @Order(8)
    @DisplayName("Get notifications by recipient returns paginated results")
    void getNotificationsByRecipient() {
        String token = adminToken();
        Long emp = createEmployee(token, "EMP-NOTIF-PAGE");
        Map<?, ?> page = notificationClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                        .queryParam("recipientId", emp)
                        .queryParam("page", 0)
                        .queryParam("size", 5).build())
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(Map.class).block();

        assertThat(page).isNotNull();
        assertThat(page.get("content")).isNotNull();
    }
}

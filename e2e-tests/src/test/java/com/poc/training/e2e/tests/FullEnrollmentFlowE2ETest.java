package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Full Enrollment Business Flow")
class FullEnrollmentFlowE2ETest extends BaseE2ETest {

    private static Long courseId;
    private static Long employeeId;
    private static Long enrollmentId;

    @Test
    @Order(1)
    @DisplayName("Admin creates a course with available seats")
    void shouldCreateCourse() {
        courseId = createCourse(adminToken(), "E2E Course Full Flow", 5);
        assertThat(courseId).isNotNull();

        Map<?, ?> course = catalogClient.get()
                .uri("/api/v1/courses/{id}", courseId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(course.get("title")).isEqualTo("E2E Course Full Flow");
        assertThat(course.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @Order(2)
    @DisplayName("Admin creates an employee")
    void shouldCreateEmployee() {
        employeeId = createEmployee(adminToken(), "EMP-E2E-001");
        assertThat(employeeId).isNotNull();

        Map<?, ?> emp = employeeClient.get()
                .uri("/api/v1/employees/{id}", employeeId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(emp.get("employeeNumber")).isEqualTo("EMP-E2E-001");
    }

    @Test
    @Order(3)
    @DisplayName("Employee enrolls in course")
    void shouldEnrollInCourse() {
        Map<?, ?> enrollment = createEnrollment(adminToken(), employeeId, courseId);
        enrollmentId = Long.valueOf(String.valueOf(enrollment.get("id")));
        assertThat(enrollmentId).isNotNull();
        assertThat(enrollment.get("status")).isEqualTo("PENDING");
    }

    @Test
    @Order(4)
    @DisplayName("Instructor approves enrollment")
    void shouldApproveEnrollment() {
        approveEnrollment(instructorToken(), enrollmentId);

        Map<?, ?> enrollment = enrollmentClient.get()
                .uri("/api/v1/enrollments/{id}", enrollmentId)
                .header("Authorization", "Bearer " + instructorToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(enrollment.get("status")).isEqualTo("APPROVED");
    }

    @Test
    @Order(5)
    @DisplayName("Instructor completes enrollment, triggers certificate generation")
    void shouldCompleteEnrollment() {
        completeEnrollment(instructorToken(), enrollmentId);

        Map<?, ?> enrollment = enrollmentClient.get()
                .uri("/api/v1/enrollments/{id}", enrollmentId)
                .header("Authorization", "Bearer " + instructorToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(enrollment.get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @Order(6)
    @DisplayName("Certificate was auto-generated after course completion")
    void shouldVerifyCertificateGenerated() {
        waitForAsyncProcessing(() -> {
            Map<?, ?> certPage = certificationClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/certificates")
                            .queryParam("employeeId", employeeId).build())
                    .header("Authorization", "Bearer " + adminToken())
                    .retrieve().bodyToMono(Map.class).block();

            assertThat(certPage).isNotNull();
            var content = (java.util.List<?>) certPage.get("content");
            assertThat(content).isNotEmpty();

            Map<?, ?> cert = (Map<?, ?>) content.get(0);
            assertThat(cert.get("certificateNumber")).asString().startsWith("CERT-");
            assertThat(Long.valueOf(String.valueOf(cert.get("employeeId")))).isEqualTo(employeeId);
            assertThat(Long.valueOf(String.valueOf(cert.get("courseId")))).isEqualTo(courseId);
        });
    }

    @Test
    @Order(7)
    @DisplayName("Notifications were generated throughout the flow")
    void shouldVerifyNotificationsGenerated() {
        Map<?, ?> notifPage = notificationClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/notifications")
                        .queryParam("recipientId", employeeId).build())
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        assertThat(notifPage).isNotNull();
        var content = (java.util.List<?>) notifPage.get("content");
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        var types = content.stream()
                .map(n -> ((Map<?, ?>) n).get("type"))
                .toList();
        assertThat(types).satisfies(t -> {
            assertThat(t).asList().contains("ENROLLMENT_CREATED");
            assertThat(t).asList().contains("COURSE_COMPLETED");
        });
    }

    @Test
    @Order(10)
    @DisplayName("Cannot enroll in same course twice (duplicate prevention)")
    void shouldPreventDuplicateEnrollment() {
        assertThatThrownBy(() ->
                createEnrollment(adminToken(), employeeId, courseId))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> {
                    var ex = (WebClientResponseException) e;
                    assertThat(ex.getStatusCode().value()).isIn(400, 409);
                });
    }

    @Test
    @Order(11)
    @DisplayName("Cannot enroll when course has no available seats")
    void shouldRejectEnrollmentWhenNoSeats() {
        String token = adminToken();
        Long noSeatsCourse = createCourse(token, "No Seats Course", 1);
        Long emp2 = createEmployee(token, "EMP-E2E-NOSEATS");
        Map<?, ?> firstEnroll = createEnrollment(token, emp2, noSeatsCourse);
        approveEnrollment(token, Long.valueOf(String.valueOf(firstEnroll.get("id"))));
        waitForEnrolledCount(noSeatsCourse, 1, token);

        Long emp3 = createEmployee(token, "EMP-E2E-NOSEATS-2");
        assertThatThrownBy(() -> createEnrollment(token, emp3, noSeatsCourse))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> {
                    var ex = (WebClientResponseException) e;
                    assertThat(ex.getStatusCode().value()).isEqualTo(400);
                });
    }

    @Test
    @Order(12)
    @DisplayName("Employee can cancel their enrollment")
    void shouldCancelEnrollment() {
        String token = adminToken();
        Long course = createCourse(token, "Cancel Course", 10);
        Long emp = createEmployee(token, "EMP-CANCEL");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));

        enrollmentClient.patch()
                .uri("/api/v1/enrollments/{id}/cancel", enrollId)
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();

        Map<?, ?> updated = enrollmentClient.get()
                .uri("/api/v1/enrollments/{id}", enrollId)
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(Map.class).block();
        assertThat(updated.get("status")).isEqualTo("CANCELLED");
    }

    @Test
    @Order(13)
    @DisplayName("Rejected enrollment flow")
    void shouldRejectEnrollment() {
        String token = adminToken();
        Long course = createCourse(token, "Reject Course", 10);
        Long emp = createEmployee(token, "EMP-REJECT");
        Map<?, ?> enroll = createEnrollment(token, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));

        enrollmentClient.patch()
                .uri("/api/v1/enrollments/{id}/reject", enrollId)
                .header("Authorization", "Bearer " + instructorToken())
                .retrieve().toBodilessEntity().block();

        Map<?, ?> updated = enrollmentClient.get()
                .uri("/api/v1/enrollments/{id}", enrollId)
                .header("Authorization", "Bearer " + token)
                .retrieve().bodyToMono(Map.class).block();
        assertThat(updated.get("status")).isEqualTo("REJECTED");
    }
}

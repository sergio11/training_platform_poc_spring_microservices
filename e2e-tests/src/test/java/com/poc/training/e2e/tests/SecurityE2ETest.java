package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Security & Authorization (RBAC)")
class SecurityE2ETest extends BaseE2ETest {

    private static Long courseId;
    private static Long employeeId;
    private static Long enrollmentId;

    @BeforeAll
    static void setupTestData() {
        String token = adminToken();
        courseId = createCourse(token, "Security Test Course", 20);
        employeeId = createEmployee(token, "EMP-SEC-001");
        Map<?, ?> enroll = createEnrollment(token, employeeId, courseId);
        enrollmentId = Long.valueOf(String.valueOf(enroll.get("id")));
    }

    @Test
    @DisplayName("Admin can access all endpoints")
    void adminAccessAll() {
        String token = adminToken();

        catalogClient.get().uri("/api/v1/courses")
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();
        catalogClient.post().uri("/api/v1/courses")
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("title", "Admin Test", "description", "x",
                        "instructor", "x", "capacity", 1))
                .retrieve().bodyToMono(Map.class).block();

        employeeClient.get().uri("/api/v1/employees")
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();

        enrollmentClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/enrollments")
                        .queryParam("employeeId", employeeId).build())
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();
    }

    @Test
    @DisplayName("Instructor can manage courses")
    void instructorManagesCourses() {
        String token = instructorToken();
        catalogClient.get().uri("/api/v1/courses")
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity().block();

        catalogClient.put()
                .uri("/api/v1/courses/{id}", courseId)
                .header("Authorization", "Bearer " + token)
                .bodyValue(Map.of("title", "Updated by Instructor", "description", "x",
                        "instructor", "Instructor", "capacity", 20))
                .retrieve().toBodilessEntity().block();
    }

    @Test
    @DisplayName("Instructor can approve enrollments")
    void instructorCanApprove() {
        String adminTok = adminToken();
        Long course = createCourse(adminTok, "Approve Test", 10);
        Long emp = createEmployee(adminTok, "EMP-APPROVE");
        Map<?, ?> enroll = createEnrollment(adminTok, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));

        enrollmentClient.patch()
                .uri("/api/v1/enrollments/{id}/approve", enrollId)
                .header("Authorization", "Bearer " + instructorToken())
                .retrieve().toBodilessEntity().block();

        Map<?, ?> result = enrollmentClient.get()
                .uri("/api/v1/enrollments/{id}", enrollId)
                .header("Authorization", "Bearer " + adminTok)
                .retrieve().bodyToMono(Map.class).block();
        assertThat(result.get("status")).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Employee cannot create courses (403)")
    void employeeCannotCreateCourse() {
        assertThatThrownBy(() -> createCourse(employeeToken(), "Fail", 1))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("Employee cannot approve enrollments (403)")
    void employeeCannotApprove() {
        assertThatThrownBy(() -> enrollmentClient.patch()
                .uri("/api/v1/enrollments/{id}/approve", enrollmentId)
                .header("Authorization", "Bearer " + employeeToken())
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("Employee can only enroll themselves")
    void employeeEnrollSelfOnly() {
        String empTok = employeeToken();
        Long empId = createEmployee(adminToken(), "EMP-SELF-ENROLL");

        assertThatThrownBy(() -> enrollmentClient.post().uri("/api/v1/enrollments")
                .header("Authorization", "Bearer " + empTok)
                .bodyValue(Map.of("employeeId", empId, "courseId", courseId))
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("Employee can cancel own enrollment")
    void employeeCancelOwn() {
        String adminTok = adminToken();
        Long course = createCourse(adminTok, "Cancel Own", 10);
        Long emp = createEmployee(adminTok, "EMP-CANCEL-OWN");
        Map<?, ?> enroll = createEnrollment(adminTok, emp, course);
        Long enrollId = Long.valueOf(String.valueOf(enroll.get("id")));

        enrollmentClient.patch()
                .uri("/api/v1/enrollments/{id}/cancel", enrollId)
                .header("Authorization", "Bearer " + adminTok)
                .retrieve().toBodilessEntity().block();

        Map<?, ?> result = enrollmentClient.get()
                .uri("/api/v1/enrollments/{id}", enrollId)
                .header("Authorization", "Bearer " + adminTok)
                .retrieve().bodyToMono(Map.class).block();
        assertThat(result.get("status")).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Request without token returns 401")
    void noTokenReturns401() {
        assertThatThrownBy(() -> catalogClient.get()
                .uri("/api/v1/courses")
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(401));
    }

    @Test
    @DisplayName("Wrong role returns 403")
    void wrongRoleReturns403() {
        assertThatThrownBy(() -> employeeClient.get()
                .uri("/api/v1/employees")
                .header("Authorization", "Bearer " + employeeToken())
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @DisplayName("Instructor cannot delete employees (403)")
    void instructorCannotDeleteEmployee() {
        Long emp = createEmployee(adminToken(), "EMP-NO-DEL");
        assertThatThrownBy(() -> employeeClient.delete()
                .uri("/api/v1/employees/{id}", emp)
                .header("Authorization", "Bearer " + instructorToken())
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }
}

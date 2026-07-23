package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Course Management CRUD")
class CourseManagementE2ETest extends BaseE2ETest {

    private static Long courseId;

    @Test
    @Order(1)
    @DisplayName("Admin can create a course")
    void adminCreatesCourse() {
        courseId = createCourse(adminToken(), "Admin Created Course", 30);
        assertThat(courseId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Employee cannot create a course (403)")
    void employeeCannotCreateCourse() {
        assertThatThrownBy(() -> createCourse(employeeToken(), "Should Fail", 10))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @Order(3)
    @DisplayName("Instructor cannot create a course (403)")
    void instructorCannotCreateCourse() {
        assertThatThrownBy(() -> createCourse(instructorToken(), "Should Fail", 10))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @Order(4)
    @DisplayName("Get course by ID returns correct data")
    void getCourseById() {
        Map<?, ?> course = catalogClient.get()
                .uri("/api/v1/courses/{id}", courseId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        assertThat(course).isNotNull();
        assertThat(course.get("id")).isEqualTo(courseId.intValue());
        assertThat(course.get("title")).isEqualTo("Admin Created Course");
    }

    @Test
    @Order(5)
    @DisplayName("List courses with pagination")
    void listCoursesPagination() {
        createCourse(adminToken(), "Pagination Course 1", 10);
        createCourse(adminToken(), "Pagination Course 2", 10);

        Map<?, ?> page = catalogClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/courses")
                        .queryParam("page", 0)
                        .queryParam("size", 2)
                        .queryParam("sortBy", "id").build())
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        assertThat(page).isNotNull();
        assertThat(page.get("content")).isNotNull();
        assertThat(((java.util.List<?>) page.get("content")).size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(6)
    @DisplayName("Filter courses by title")
    void filterByTitle() {
        createCourse(adminToken(), "Unique Title XYZ", 5);

        Map<?, ?> page = catalogClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/courses")
                        .queryParam("title", "Unique Title XYZ").build())
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        var content = (java.util.List<?>) page.get("content");
        assertThat(content).isNotEmpty();
        assertThat(((Map<?, ?>) content.get(0)).get("title")).isEqualTo("Unique Title XYZ");
    }

    @Test
    @Order(7)
    @DisplayName("Filter courses by status")
    void filterByStatus() {
        Map<?, ?> page = catalogClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/courses")
                        .queryParam("status", "ACTIVE").build())
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        var content = (java.util.List<?>) page.get("content");
        assertThat(content).isNotEmpty();
        content.forEach(c ->
                assertThat(((Map<?, ?>) c).get("status")).isEqualTo("ACTIVE"));
    }

    @Test
    @Order(8)
    @DisplayName("Instructor can update a course")
    void instructorUpdatesCourse() {
        catalogClient.put()
                .uri("/api/v1/courses/{id}", courseId)
                .header("Authorization", "Bearer " + instructorToken())
                .bodyValue(Map.of("title", "Updated Title", "description", "Updated",
                        "instructor", "Instructor", "capacity", 50))
                .retrieve().toBodilessEntity().block();

        Map<?, ?> course = catalogClient.get()
                .uri("/api/v1/courses/{id}", courseId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(course.get("title")).isEqualTo("Updated Title");
    }

    @Test
    @Order(9)
    @DisplayName("Employee cannot update a course (403)")
    void employeeCannotUpdateCourse() {
        assertThatThrownBy(() -> catalogClient.put()
                .uri("/api/v1/courses/{id}", courseId)
                .header("Authorization", "Bearer " + employeeToken())
                .bodyValue(Map.of("title", "Fail", "description", "x",
                        "instructor", "x", "capacity", 1))
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }

    @Test
    @Order(10)
    @DisplayName("Admin can deactivate a course")
    void adminDeactivatesCourse() {
        Long course = createCourse(adminToken(), "To Deactivate", 10);

        catalogClient.delete()
                .uri("/api/v1/courses/{id}", course)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().toBodilessEntity().block();

        Map<?, ?> result = catalogClient.get()
                .uri("/api/v1/courses/{id}", course)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(result.get("status")).isEqualTo("INACTIVE");
    }

    @Test
    @Order(11)
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
    @Order(12)
    @DisplayName("Invalid request body returns 400")
    void validateRequestFields() {
        assertThatThrownBy(() -> catalogClient.post()
                .uri("/api/v1/courses")
                .header("Authorization", "Bearer " + adminToken())
                .bodyValue(Map.of("title", "", "capacity", 0))
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(13)
    @DisplayName("Non-existent course returns 404")
    void return404ForNonExistentCourse() {
        assertThatThrownBy(() -> catalogClient.get()
                .uri("/api/v1/courses/{id}", 999999)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(404));
    }
}

package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Edge Cases & Concurrency")
class EdgeCaseE2ETest extends BaseE2ETest {

    @Test
    @Order(1)
    @DisplayName("Enroll employee in multiple courses simultaneously")
    void concurrentEnrollmentInDifferentCourses() throws Exception {
        String token = adminToken();
        Long emp = createEmployee(token, "EMP-CONC-001");
        int courseCount = 3;

        ExecutorService executor = Executors.newFixedThreadPool(courseCount);
        CountDownLatch latch = new CountDownLatch(courseCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < courseCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    Long course = createCourse(token, "Concurrent Course " + idx, 10);
                    createEnrollment(token, emp, course);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(courseCount);
        assertThat(failCount.get()).isEqualTo(0);
    }

    @Test
    @Order(2)
    @DisplayName("Cannot enroll when course is at full capacity")
    void shouldRejectEnrollmentAtFullCapacity() {
        String token = adminToken();
        Long course = createCourse(token, "Full Capacity Course", 1);
        Long emp1 = createEmployee(token, "EMP-FULL-001");
        Long emp2 = createEmployee(token, "EMP-FULL-002");

        Map<?, ?> firstEnroll = createEnrollment(token, emp1, course);
        approveEnrollment(token, Long.valueOf(String.valueOf(firstEnroll.get("id"))));
        waitForEnrolledCount(course, 1, token);

        assertThatThrownBy(() -> createEnrollment(token, emp2, course))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(3)
    @DisplayName("Invalid request body returns 400")
    void validateRequestFields() {
        assertThatThrownBy(() -> catalogClient.post()
                .uri("/api/v1/courses")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("title", "", "capacity", 0))
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @Order(4)
    @DisplayName("Non-existent resource returns 404")
    void return404ForNonExistentResource() {
        assertThatThrownBy(() -> catalogClient.get()
                .uri("/api/v1/courses/{id}", 999999)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @Order(5)
    @DisplayName("Unauthorized access returns 401")
    void unauthorizedReturns401() {
        assertThatThrownBy(() -> catalogClient.get()
                .uri("/api/v1/courses")
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(401));
    }
}

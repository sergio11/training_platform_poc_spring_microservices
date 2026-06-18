package com.poc.training.enrollment.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class TrainingCatalogClient {

    private final WebClient webClient;

    public TrainingCatalogClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8081")
                .build();
    }

    @CircuitBreaker(name = "catalogService", fallbackMethod = "getCourseFallback")
    @Retry(name = "catalogService")
    public CourseDTO getCourse(Long courseId) {
        log.info("Fetching course with id: {}", courseId);
        return webClient.get()
                .uri("/api/v1/courses/{id}", courseId)
                .retrieve()
                .bodyToMono(CourseDTO.class)
                .block();
    }

    public CourseDTO getCourseFallback(Long courseId, Throwable throwable) {
        log.error("Fallback called for getCourse with id: {}. Error: {}", courseId, throwable.getMessage());
        return CourseDTO.builder()
                .id(courseId)
                .title("Unknown Course")
                .capacity(0)
                .enrolledCount(0)
                .status("UNKNOWN")
                .build();
    }
}

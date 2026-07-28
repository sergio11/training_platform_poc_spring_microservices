package com.poc.training.enrollment.client;

import com.poc.training.shared.dto.CourseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.poc.training.shared.exception.ServiceUnavailableException;

import java.time.Duration;

@Slf4j
@Component
public class TrainingCatalogClient {

    private final WebClient webClient;

    public TrainingCatalogClient(WebClient.Builder webClientBuilder,
            @Value("${client.catalog.url:http://localhost:8081}") String catalogBaseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(catalogBaseUrl)
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
                .block(Duration.ofSeconds(5));
    }

    public CourseDTO getCourseFallback(Long courseId, Throwable throwable) {
        log.error("Fallback called for getCourse with id: {}. Error: {}", courseId, throwable.getMessage());
        throw new ServiceUnavailableException("Catalog Service unavailable for course: " + courseId, throwable);
    }

}

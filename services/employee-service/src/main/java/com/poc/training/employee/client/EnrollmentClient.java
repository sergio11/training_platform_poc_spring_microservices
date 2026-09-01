package com.poc.training.employee.client;

import com.poc.training.shared.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
public class EnrollmentClient {

    private final WebClient webClient;

    public EnrollmentClient(WebClient.Builder webClientBuilder,
            @Value("${client.enrollment.url:http://localhost:8082}") String enrollmentBaseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(enrollmentBaseUrl)
                .build();
    }

    @CircuitBreaker(name = "enrollmentService", fallbackMethod = "hasActiveEnrollmentsFallback")
    @Retry(name = "enrollmentService")
    public boolean hasActiveEnrollments(Long employeeId) {
        try {
            return Boolean.TRUE.equals(webClient.get()
                    .uri("/api/v1/enrollments/active/{employeeId}", employeeId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block(Duration.ofSeconds(5)));
        } catch (Exception e) {
            log.warn("Could not check active enrollments for employee {}: {}", employeeId, e.getMessage());
            return false;
        }
    }

    public boolean hasActiveEnrollmentsFallback(Long employeeId, Throwable throwable) {
        log.error("Fallback called for hasActiveEnrollments. Employee: {}. Error: {}", employeeId, throwable.getMessage());
        throw new ServiceUnavailableException("Enrollment Service unavailable for employee: " + employeeId, throwable);
    }
}

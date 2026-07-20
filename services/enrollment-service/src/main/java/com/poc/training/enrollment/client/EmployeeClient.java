package com.poc.training.enrollment.client;

import com.poc.training.shared.dto.EmployeeDTO;
import com.poc.training.shared.exception.ResourceNotFoundException;
import com.poc.training.shared.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Slf4j
@Component
public class EmployeeClient {

    private final WebClient webClient;

    public EmployeeClient(WebClient.Builder webClientBuilder,
            @Value("${client.employee.url:http://localhost:8085}") String employeeBaseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(employeeBaseUrl)
                .build();
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "existsByIdFallback")
    @Retry(name = "employeeService")
    public boolean existsById(Long employeeId) {
        return Boolean.TRUE.equals(webClient.get()
                .uri("/api/v1/employees/{id}/exists", employeeId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(Duration.ofSeconds(5)));
    }

    public boolean existsByIdFallback(Long employeeId, Throwable throwable) {
        log.error("Fallback called for existsById with employee: {}. Error: {}", employeeId, throwable.getMessage());
        throw new ServiceUnavailableException("Employee Service unavailable for employee: " + employeeId, throwable);
    }

    @CircuitBreaker(name = "employeeService", fallbackMethod = "getEmployeeFallback")
    @Retry(name = "employeeService")
    public EmployeeDTO getEmployee(Long employeeId) {
        return webClient.get()
                .uri("/api/v1/employees/{id}", employeeId)
                .retrieve()
                .bodyToMono(EmployeeDTO.class)
                .block(Duration.ofSeconds(5));
    }

    public EmployeeDTO getEmployeeFallback(Long employeeId, Throwable throwable) {
        log.error("Fallback called for getEmployee with id: {}. Error: {}", employeeId, throwable.getMessage());
        throw new ServiceUnavailableException("Employee Service unavailable for employee: " + employeeId, throwable);
    }
}

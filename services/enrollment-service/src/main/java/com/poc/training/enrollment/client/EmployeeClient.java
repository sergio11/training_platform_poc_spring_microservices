package com.poc.training.enrollment.client;

import com.poc.training.shared.dto.EmployeeDTO;
import com.poc.training.shared.exception.ResourceNotFoundException;
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

    public boolean existsById(Long employeeId) {
        try {
            return Boolean.TRUE.equals(webClient.get()
                    .uri("/api/v1/employees/{id}/exists", employeeId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block(Duration.ofSeconds(5)));
        } catch (WebClientResponseException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.warn("Failed to check employee existence for {}: {}", employeeId, e.getMessage());
            return false;
        }
    }

    public EmployeeDTO getEmployee(Long employeeId) {
        try {
            return webClient.get()
                    .uri("/api/v1/employees/{id}", employeeId)
                    .retrieve()
                    .bodyToMono(EmployeeDTO.class)
                    .block(Duration.ofSeconds(5));
        } catch (WebClientResponseException.NotFound e) {
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
        }
    }
}

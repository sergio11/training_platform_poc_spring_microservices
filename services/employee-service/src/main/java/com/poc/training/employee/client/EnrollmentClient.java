package com.poc.training.employee.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class EnrollmentClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public EnrollmentClient(@Value("${client.enrollment.url:http://localhost:8082}") String enrollmentBaseUrl) {
        this.baseUrl = enrollmentBaseUrl;
        this.restTemplate = createDefaultRestTemplate();
    }

    EnrollmentClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createDefaultRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    public boolean hasActiveEnrollments(Long employeeId) {
        try {
            Boolean result = restTemplate.getForObject(
                    baseUrl + "/api/v1/enrollments/active/{employeeId}", Boolean.class, employeeId);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Could not check active enrollments for employee {}: {}", employeeId, e.getMessage());
            return false;
        }
    }
}

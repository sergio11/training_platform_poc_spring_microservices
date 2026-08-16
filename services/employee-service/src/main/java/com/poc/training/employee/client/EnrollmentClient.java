package com.poc.training.employee.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Component
public class EnrollmentClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public EnrollmentClient(@Value("${client.enrollment.url:http://localhost:8082}") String enrollmentBaseUrl) {
        this.baseUrl = enrollmentBaseUrl;
        this.restTemplate = createRestTemplateWithTokenRelay();
    }

    EnrollmentClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplateWithTokenRelay() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(List.of(tokenRelayInterceptor()));
        return restTemplate;
    }

    private static ClientHttpRequestInterceptor tokenRelayInterceptor() {
        return (request, body, execution) -> {
            String authHeader = extractAuthHeader();
            if (authHeader != null) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeader);
            }
            return execution.execute(request, body);
        };
    }

    private static String extractAuthHeader() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest servletRequest = servletAttrs.getRequest();
                return servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            }
        } catch (Exception e) {
            log.debug("Could not extract auth header: {}", e.getMessage());
        }
        return null;
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

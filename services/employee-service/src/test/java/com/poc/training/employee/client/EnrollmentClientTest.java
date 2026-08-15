package com.poc.training.employee.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class EnrollmentClientTest {

    @Test
    void shouldReturnTrueWhenHasActiveEnrollments() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        when(restTemplate.getForObject(
                eq("http://localhost:8082/api/v1/enrollments/active/{employeeId}"),
                eq(Boolean.class),
                eq(1L)))
                .thenReturn(true);

        EnrollmentClient client = new EnrollmentClient("http://localhost:8082", restTemplate);
        boolean result = client.hasActiveEnrollments(1L);
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenResponseIsNull() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        when(restTemplate.getForObject(
                eq("http://localhost:8082/api/v1/enrollments/active/{employeeId}"),
                eq(Boolean.class),
                eq(1L)))
                .thenReturn(null);

        EnrollmentClient client = new EnrollmentClient("http://localhost:8082", restTemplate);
        boolean result = client.hasActiveEnrollments(1L);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenExceptionOccurs() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        when(restTemplate.getForObject(
                eq("http://localhost:8082/api/v1/enrollments/active/{employeeId}"),
                eq(Boolean.class),
                eq(1L)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        EnrollmentClient client = new EnrollmentClient("http://localhost:8082", restTemplate);
        boolean result = client.hasActiveEnrollments(1L);
        assertFalse(result);
    }
}

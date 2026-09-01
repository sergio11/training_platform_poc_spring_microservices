package com.poc.training.employee.client;

import com.poc.training.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentClientTest {

    private EnrollmentClient createClient(String baseUrl) {
        WebClient.Builder builder = WebClient.builder();
        return new EnrollmentClient(builder, baseUrl);
    }

    @Test
    void shouldCreateClientWithValidUrl() {
        EnrollmentClient client = createClient("http://localhost:8082");
        assertNotNull(client);
    }

    @Test
    void shouldReturnFalseWhenExceptionOccurs() {
        EnrollmentClient client = createClient("http://localhost:9999");
        boolean result = client.hasActiveEnrollments(1L);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenServiceIsUnavailable() {
        EnrollmentClient client = createClient("http://localhost:9999");
        boolean result = client.hasActiveEnrollments(42L);
        assertFalse(result);
    }

    @Test
    void shouldHandleMultipleCallsGracefully() {
        EnrollmentClient client = createClient("http://localhost:9999");
        assertFalse(client.hasActiveEnrollments(1L));
        assertFalse(client.hasActiveEnrollments(2L));
        assertFalse(client.hasActiveEnrollments(3L));
    }
}

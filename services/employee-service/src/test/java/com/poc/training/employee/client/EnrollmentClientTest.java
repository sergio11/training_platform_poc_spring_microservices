package com.poc.training.employee.client;

import com.poc.training.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient.Builder chainedBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private EnrollmentClient enrollmentClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(chainedBuilder);
        when(chainedBuilder.build()).thenReturn(webClient);
        enrollmentClient = new EnrollmentClient(webClientBuilder, "http://localhost:8082");
    }

    @Test
    void shouldCreateClientWithValidUrl() {
        assertNotNull(enrollmentClient);
    }

    @Test
    void shouldReturnTrueWhenHasActiveEnrollments() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));

        assertTrue(enrollmentClient.hasActiveEnrollments(1L));
    }

    @Test
    void shouldReturnFalseWhenNoActiveEnrollments() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(99L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.just(false));

        assertFalse(enrollmentClient.hasActiveEnrollments(99L));
    }

    @Test
    void shouldReturnFalseWhenResponseIsNull() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.empty());

        assertFalse(enrollmentClient.hasActiveEnrollments(1L));
    }

    @Test
    void shouldReturnFalseWhenExceptionOccurs() {
        when(webClient.get()).thenThrow(new RuntimeException("Connection refused"));

        assertFalse(enrollmentClient.hasActiveEnrollments(1L));
    }

    @Test
    void shouldThrowInHasActiveEnrollmentsFallback() {
        assertThrows(ServiceUnavailableException.class, () ->
                enrollmentClient.hasActiveEnrollmentsFallback(1L, new RuntimeException("Connection refused")));
    }

    @Test
    void shouldThrowInHasActiveEnrollmentsFallbackWithNullId() {
        assertThrows(ServiceUnavailableException.class, () ->
                enrollmentClient.hasActiveEnrollmentsFallback(null, new RuntimeException("Error")));
    }
}

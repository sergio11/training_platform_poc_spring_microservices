package com.poc.training.enrollment.client;

import com.poc.training.shared.dto.EmployeeDTO;
import com.poc.training.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeClientTest {

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

    private EmployeeClient employeeClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(chainedBuilder);
        when(chainedBuilder.build()).thenReturn(webClient);
        employeeClient = new EmployeeClient(webClientBuilder, "http://localhost:8085");
    }

    @Test
    void shouldReturnTrueWhenEmployeeExists() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));

        assertTrue(employeeClient.existsById(1L));
    }

    @Test
    void shouldReturnFalseWhenEmployeeDoesNotExist() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(99L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.just(false));

        assertFalse(employeeClient.existsById(99L));
    }

    @Test
    void shouldReturnFalseWhenResponseIsNull() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Boolean.class)).thenReturn(Mono.empty());

        assertFalse(employeeClient.existsById(1L));
    }

    @Test
    void shouldThrowInExistsByIdFallback() {
        assertThrows(ServiceUnavailableException.class, () ->
                employeeClient.existsByIdFallback(1L, new RuntimeException("Connection refused")));
    }

    @Test
    void shouldGetEmployee() {
        EmployeeDTO dto = EmployeeDTO.builder().id(1L).fullName("Alice").employeeNumber("EMP001").build();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EmployeeDTO.class)).thenReturn(Mono.just(dto));

        EmployeeDTO result = employeeClient.getEmployee(1L);
        assertNotNull(result);
        assertEquals("Alice", result.getFullName());
    }

    @Test
    void shouldGetEmployeeReturnNullWhenNotFound() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(99L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EmployeeDTO.class)).thenReturn(Mono.empty());

        EmployeeDTO result = employeeClient.getEmployee(99L);
        assertNull(result);
    }

    @Test
    void shouldThrowInGetEmployeeFallback() {
        assertThrows(ServiceUnavailableException.class, () ->
                employeeClient.getEmployeeFallback(1L, new RuntimeException("Timeout")));
    }

    @Test
    void shouldThrowInGetEmployeeFallbackWithNullId() {
        assertThrows(ServiceUnavailableException.class, () ->
                employeeClient.getEmployeeFallback(null, new RuntimeException("Error")));
    }

    @Test
    void shouldThrowInExistsByIdFallbackWithNullId() {
        assertThrows(ServiceUnavailableException.class, () ->
                employeeClient.existsByIdFallback(null, new RuntimeException("Error")));
    }
}

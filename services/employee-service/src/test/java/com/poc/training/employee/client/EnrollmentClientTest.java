package com.poc.training.employee.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class EnrollmentClientTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

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

    @Test
    void shouldCreateRestTemplateWithTokenRelay() {
        EnrollmentClient client = new EnrollmentClient("http://localhost:8082");
        assertNotNull(client);
    }

    @Test
    void shouldPropagateAuthHeaderFromRequestContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        EnrollmentClient client = new EnrollmentClient("http://localhost:8082");
        ClientHttpRequestInterceptor interceptor = getInterceptor(client);

        HttpHeaders outgoingHeaders = new HttpHeaders();
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeaders()).thenReturn(outgoingHeaders);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse httpResponse = mock(ClientHttpResponse.class);
        when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

        interceptor.intercept(httpRequest, new byte[0], execution);

        assertEquals("Bearer test-token-123", outgoingHeaders.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldNotSetAuthHeaderWhenNoRequestContext() throws Exception {
        RequestContextHolder.resetRequestAttributes();

        EnrollmentClient client = new EnrollmentClient("http://localhost:8082");
        ClientHttpRequestInterceptor interceptor = getInterceptor(client);

        HttpHeaders outgoingHeaders = new HttpHeaders();
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeaders()).thenReturn(outgoingHeaders);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse httpResponse = mock(ClientHttpResponse.class);
        when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

        interceptor.intercept(httpRequest, new byte[0], execution);

        assertNull(outgoingHeaders.get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldNotSetAuthHeaderWhenRequestHasNoAuthHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        EnrollmentClient client = new EnrollmentClient("http://localhost:8082");
        ClientHttpRequestInterceptor interceptor = getInterceptor(client);

        HttpHeaders outgoingHeaders = new HttpHeaders();
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(httpRequest.getHeaders()).thenReturn(outgoingHeaders);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse httpResponse = mock(ClientHttpResponse.class);
        when(execution.execute(httpRequest, new byte[0])).thenReturn(httpResponse);

        interceptor.intercept(httpRequest, new byte[0], execution);

        assertNull(outgoingHeaders.get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldReturnFalseWhenRequestContextHolderHasNoAttributes() {
        RequestContextHolder.setRequestAttributes(null);
        EnrollmentClient client = new EnrollmentClient("http://localhost:8082");
        boolean result = client.hasActiveEnrollments(1L);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenExtractAuthHeaderThrows() {
        try (MockedStatic<RequestContextHolder> mocked = mockStatic(RequestContextHolder.class)) {
            mocked.when(RequestContextHolder::getRequestAttributes)
                    .thenThrow(new RuntimeException("RequestContext unavailable"));

            EnrollmentClient client = new EnrollmentClient("http://localhost:8082");
            boolean result = client.hasActiveEnrollments(1L);
            assertFalse(result);
        }
    }

    @SuppressWarnings("unchecked")
    private ClientHttpRequestInterceptor getInterceptor(EnrollmentClient client) throws Exception {
        Field restTemplateField = EnrollmentClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) restTemplateField.get(client);
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        assertNotNull(interceptors);
        assertFalse(interceptors.isEmpty());
        return interceptors.get(0);
    }
}

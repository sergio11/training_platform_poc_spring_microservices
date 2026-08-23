package com.poc.training.shared.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturn404ForResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Course not found with id: 42");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Not Found", response.getBody().get("error"));
        assertEquals("Course not found with id: 42", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void shouldReturn400ForBusinessException() {
        BusinessException ex = new BusinessException("No available seats for course: 1");

        ResponseEntity<Map<String, Object>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("No available seats for course: 1", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void shouldReturn400ForValidationErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("request", "title", "Title is required");
        FieldError fieldError2 = new FieldError("request", "instructor", "Instructor is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Validation Failed", response.getBody().get("error"));
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("title: Title is required"));
        assertTrue(message.contains("instructor: Instructor is required"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void shouldReturn400ForMalformedJson() {
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException("Malformed JSON", null);

        ResponseEntity<Map<String, Object>> response = handler.handleMalformedJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("Malformed request body", response.getBody().get("message"));
    }

    @Test
    void shouldReturn403ForAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().get("status"));
        assertEquals("Forbidden", response.getBody().get("error"));
        assertEquals("You don't have permission to access this resource", response.getBody().get("message"));
    }

    @Test
    void shouldReturn405ForMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().get("status"));
        assertEquals("Method Not Allowed", response.getBody().get("error"));
        assertTrue(((String) response.getBody().get("message")).contains("DELETE"));
    }

    @Test
    void shouldReturn500ForGenericException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("An unexpected error occurred. Please contact support.", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void shouldReturn503ForServiceUnavailable() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Keycloak is down");

        ResponseEntity<Map<String, Object>> response = handler.handleServiceUnavailable(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().get("status"));
        assertEquals("Service Unavailable", response.getBody().get("error"));
        assertEquals("Keycloak is down", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void shouldHandleConstraintViolation() {
        jakarta.validation.ConstraintViolation<?> violation = mock(jakarta.validation.ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        jakarta.validation.ConstraintViolationException ex = mock(jakarta.validation.ConstraintViolationException.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));

        var response = handler.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
    }

    @Test
    void shouldHandleMultipleConstraintViolations() {
        jakarta.validation.ConstraintViolation<?> v1 = mock(jakarta.validation.ConstraintViolation.class);
        when(v1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(v1.getMessage()).thenReturn("must not be blank");

        jakarta.validation.ConstraintViolation<?> v2 = mock(jakarta.validation.ConstraintViolation.class);
        when(v2.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(v2.getMessage()).thenReturn("size must be between 2 and 50");

        jakarta.validation.ConstraintViolationException ex = mock(jakarta.validation.ConstraintViolationException.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(v1, v2));

        var response = handler.handleConstraintViolation(ex);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        String message = (String) body.get("message");
        assertTrue(message.contains(";"));
    }

    @Test
    void shouldHandleDataIntegrityViolation() {
        var ex = new org.springframework.dao.DataIntegrityViolationException("Duplicate key");

        var response = handler.handleDataIntegrityViolation(ex);

        assertEquals(409, response.getStatusCode().value());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.get("status"));
    }

    @Test
    void shouldHandleCannotAcquireLock() {
        var ex = new org.springframework.dao.CannotAcquireLockException("Lock timeout");

        var response = handler.handleCannotAcquireLock(ex);

        assertEquals(409, response.getStatusCode().value());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.get("status"));
    }
}

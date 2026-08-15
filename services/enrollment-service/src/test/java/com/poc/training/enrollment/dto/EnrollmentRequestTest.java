package com.poc.training.enrollment.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnrollmentRequestTest {

    @Test
    void shouldCreateWithAllArgsConstructor() {
        EnrollmentRequest request = new EnrollmentRequest(1L, 1L);
        assertEquals(1L, request.getEmployeeId());
        assertEquals(1L, request.getCourseId());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        EnrollmentRequest request = new EnrollmentRequest();
        assertNotNull(request);
    }

    @Test
    void shouldSetAndGetAllFields() {
        EnrollmentRequest request = new EnrollmentRequest();
        request.setEmployeeId(1L);
        request.setCourseId(2L);

        assertEquals(1L, request.getEmployeeId());
        assertEquals(2L, request.getCourseId());
    }

    @Test
    void shouldHaveCorrectToString() {
        EnrollmentRequest request = new EnrollmentRequest(1L, 1L);
        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        EnrollmentRequest r1 = new EnrollmentRequest(1L, 1L);
        EnrollmentRequest r2 = new EnrollmentRequest(1L, 1L);
        EnrollmentRequest r3 = new EnrollmentRequest(1L, 2L);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }
}

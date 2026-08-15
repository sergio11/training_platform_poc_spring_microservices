package com.poc.training.enrollment.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EnrollmentResponseTest {

    @Test
    void shouldCreateWithBuilder() {
        LocalDateTime now = LocalDateTime.now();
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("PENDING")
                .enrolledAt(now)
                .completedAt(null)
                .build();

        assertEquals(1L, response.getId());
        assertEquals(1L, response.getEmployeeId());
        assertEquals(1L, response.getCourseId());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getEnrolledAt());
        assertNull(response.getCompletedAt());
    }

    @Test
    void shouldHaveCorrectToString() {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("PENDING")
                .build();

        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("PENDING"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        EnrollmentResponse r1 = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").enrolledAt(now).build();
        EnrollmentResponse r2 = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").enrolledAt(now).build();
        EnrollmentResponse r3 = EnrollmentResponse.builder()
                .id(2L).employeeId(1L).courseId(1L).status("APPROVED").enrolledAt(now).build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }
}

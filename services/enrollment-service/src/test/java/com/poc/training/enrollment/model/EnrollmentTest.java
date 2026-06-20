package com.poc.training.enrollment.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EnrollmentTest {

    @Test
    void shouldSetDefaultValuesOnPrePersist() {
        Enrollment enrollment = Enrollment.builder()
                .employeeId(1L)
                .courseId(1L)
                .build();

        enrollment.onCreate();

        assertEquals(EnrollmentStatus.PENDING, enrollment.getStatus());
        assertNotNull(enrollment.getEnrolledAt());
    }

    @Test
    void shouldCreateEnrollmentWithBuilder() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.APPROVED)
                .enrolledAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        assertEquals(1L, enrollment.getId());
        assertEquals(1L, enrollment.getEmployeeId());
        assertEquals(1L, enrollment.getCourseId());
        assertEquals(EnrollmentStatus.APPROVED, enrollment.getStatus());
        assertNotNull(enrollment.getEnrolledAt());
        assertNotNull(enrollment.getCompletedAt());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        Enrollment enrollment = new Enrollment();
        assertNotNull(enrollment);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        Enrollment enrollment = new Enrollment(1L, 1L, 2L, EnrollmentStatus.PENDING, LocalDateTime.now(), null);
        assertEquals(EnrollmentStatus.PENDING, enrollment.getStatus());
    }
}

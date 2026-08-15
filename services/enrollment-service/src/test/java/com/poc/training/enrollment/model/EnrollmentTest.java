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
        Enrollment enrollment = new Enrollment(1L, null, 1L, 2L, EnrollmentStatus.PENDING, LocalDateTime.now(), null);
        assertEquals(EnrollmentStatus.PENDING, enrollment.getStatus());
    }

    @Test
    void shouldHaveCorrectToString() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        String str = enrollment.toString();
        assertNotNull(str);
        assertTrue(str.contains("PENDING"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Enrollment e1 = Enrollment.builder()
                .id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
        Enrollment e2 = Enrollment.builder()
                .id(1L).employeeId(2L).courseId(2L).status(EnrollmentStatus.APPROVED).build();
        Enrollment e3 = Enrollment.builder()
                .id(2L).employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
    }

    @Test
    void shouldNotSetNonNullStatusOnPrePersist() {
        Enrollment enrollment = Enrollment.builder()
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.APPROVED)
                .build();

        enrollment.onCreate();

        assertEquals(EnrollmentStatus.APPROVED, enrollment.getStatus());
        assertNotNull(enrollment.getEnrolledAt());
    }
}

package com.poc.training.enrollment.integration;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.enrollment.service.EnrollmentService;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class EnrollmentFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
    }

    @Test
    void shouldCreateAndTransitionEnrollment() {
        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        assertEquals(EnrollmentStatus.PENDING, enrollment.getStatus());
        assertNotNull(enrollment.getId());
    }

    @Test
    void shouldFindEnrollmentsByEmployeeId() {
        enrollmentRepository.save(Enrollment.builder().employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build());
        enrollmentRepository.save(Enrollment.builder().employeeId(1L).courseId(2L).status(EnrollmentStatus.APPROVED).build());

        var result = enrollmentService.findByEmployeeId(1L, PageRequest.of(0, 10));

        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldTransitionFromPendingToApproved() {
        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        saved.setStatus(EnrollmentStatus.APPROVED);
        enrollmentRepository.save(saved);

        Enrollment updated = enrollmentRepository.findById(saved.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.APPROVED, updated.getStatus());
    }

    @Test
    void shouldTransitionToCompleted() {
        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .employeeId(1L).courseId(1L).status(EnrollmentStatus.APPROVED).build());

        saved.setStatus(EnrollmentStatus.COMPLETED);
        saved.setCompletedAt(LocalDateTime.now());
        enrollmentRepository.save(saved);

        Enrollment updated = enrollmentRepository.findById(saved.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.COMPLETED, updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void shouldTransitionToCancelled() {
        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        saved.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(saved);

        Enrollment updated = enrollmentRepository.findById(saved.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void shouldTransitionToRejected() {
        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        saved.setStatus(EnrollmentStatus.REJECTED);
        enrollmentRepository.save(saved);

        Enrollment updated = enrollmentRepository.findById(saved.getId()).orElseThrow();
        assertEquals(EnrollmentStatus.REJECTED, updated.getStatus());
    }

    @Test
    void shouldFindExistingEnrollmentWithLock() {
        enrollmentRepository.save(Enrollment.builder()
                .employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        var existing = enrollmentRepository.findByEmployeeIdAndCourseIdAndStatusInWithLock(
                1L, 1L, java.util.List.of(EnrollmentStatus.PENDING, EnrollmentStatus.APPROVED));

        assertFalse(existing.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenNoActiveEnrollment() {
        var existing = enrollmentRepository.findByEmployeeIdAndCourseIdAndStatusInWithLock(
                1L, 1L, java.util.List.of(EnrollmentStatus.PENDING, EnrollmentStatus.APPROVED));

        assertTrue(existing.isEmpty());
    }
}

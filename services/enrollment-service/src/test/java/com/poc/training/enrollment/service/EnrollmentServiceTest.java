package com.poc.training.enrollment.service;

import com.poc.training.shared.dto.CourseDTO;
import com.poc.training.enrollment.client.TrainingCatalogClient;
import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import com.poc.training.shared.event.EnrollmentCreatedEvent;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TrainingCatalogClient trainingCatalogClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void shouldFindEnrollmentsByEmployeeId() {
        Enrollment e1 = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
        Enrollment e2 = Enrollment.builder().id(2L).employeeId(1L).courseId(2L).status(EnrollmentStatus.APPROVED).build();
        when(enrollmentRepository.findByEmployeeId(eq(1L), any(PageRequest.class))).thenReturn(new PageImpl<>(Arrays.asList(e1, e2)));

        var result = enrollmentService.findByEmployeeId(1L, PageRequest.of(0, 10));
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldFindEnrollmentById() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        EnrollmentResponse result = enrollmentService.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowExceptionWhenEnrollmentNotFound() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.findById(99L));
    }

    @Test
    void shouldEnrollEmployee() {
        CourseDTO course = CourseDTO.builder().id(1L).capacity(30).enrolledCount(0).status("ACTIVE").build();
        when(trainingCatalogClient.getCourse(1L)).thenReturn(course);
        when(enrollmentRepository.existsByEmployeeIdAndCourseIdAndStatusIn(eq(1L), eq(1L), anyList())).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EnrollmentResponse result = enrollmentService.enroll(1L, 1L);
        assertNotNull(result);
        assertEquals(EnrollmentStatus.PENDING.name(), result.getStatus());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowExceptionWhenAlreadyEnrolled() {
        CourseDTO course = CourseDTO.builder().id(1L).capacity(30).enrolledCount(0).status("ACTIVE").build();
        when(trainingCatalogClient.getCourse(1L)).thenReturn(course);
        when(enrollmentRepository.existsByEmployeeIdAndCourseIdAndStatusIn(eq(1L), eq(1L), anyList())).thenReturn(true);

        assertThrows(BusinessException.class, () -> enrollmentService.enroll(1L, 1L));
    }

    @Test
    void shouldThrowExceptionWhenNoSeats() {
        CourseDTO course = CourseDTO.builder().id(1L).capacity(30).enrolledCount(30).status("ACTIVE").build();
        when(trainingCatalogClient.getCourse(1L)).thenReturn(course);
        when(enrollmentRepository.existsByEmployeeIdAndCourseIdAndStatusIn(eq(1L), eq(1L), anyList())).thenReturn(false);

        assertThrows(BusinessException.class, () -> enrollmentService.enroll(1L, 1L));
    }

    @Test
    void shouldApproveEnrollment() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = enrollmentService.approve(1L);
        assertEquals(EnrollmentStatus.APPROVED.name(), result.getStatus());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowExceptionWhenApprovingNonPendingEnrollment() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.APPROVED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessException.class, () -> enrollmentService.approve(1L));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowExceptionWhenApprovingCompletedEnrollment() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.COMPLETED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessException.class, () -> enrollmentService.approve(1L));
    }

    @Test
    void shouldCompleteEnrollment() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.APPROVED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = enrollmentService.complete(1L);
        assertEquals(EnrollmentStatus.COMPLETED.name(), result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowExceptionWhenCompletingNonApprovedEnrollment() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.PENDING).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessException.class, () -> enrollmentService.complete(1L));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowExceptionWhenCompletingAlreadyCompletedEnrollment() {
        Enrollment e = Enrollment.builder().id(1L).employeeId(1L).courseId(1L).status(EnrollmentStatus.COMPLETED).build();
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessException.class, () -> enrollmentService.complete(1L));
    }
}

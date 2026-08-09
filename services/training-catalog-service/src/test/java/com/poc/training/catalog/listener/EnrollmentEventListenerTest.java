package com.poc.training.catalog.listener;

import com.poc.training.catalog.model.ProcessedEvent;
import com.poc.training.catalog.repository.ProcessedEventRepository;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import com.poc.training.shared.event.EnrollmentCancelledEvent;
import com.poc.training.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentEventListenerTest {

    @Mock
    private CourseService courseService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private EnrollmentEventListener enrollmentEventListener;

    @Test
    void shouldProcessEnrollmentApprovedEvent() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("evt-1")
                .courseId(10L)
                .employeeId(1L)
                .enrollmentId(100L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        enrollmentEventListener.handleEnrollmentApproved(event);

        verify(courseService).incrementEnrolledCount(10L);
        verify(processedEventRepository).save(argThat(pe ->
                pe.getEventId().equals("evt-1") && pe.getEventType().equals("EnrollmentApprovedEvent")));
    }

    @Test
    void shouldSkipAlreadyProcessedApprovedEvent() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("evt-dup")
                .courseId(10L)
                .employeeId(1L)
                .enrollmentId(100L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-dup")).thenReturn(true);

        enrollmentEventListener.handleEnrollmentApproved(event);

        verify(courseService, never()).incrementEnrolledCount(anyLong());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldHandleBusinessExceptionOnApproved() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("evt-biz")
                .courseId(10L)
                .employeeId(1L)
                .enrollmentId(100L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-biz")).thenReturn(false);
        doThrow(new BusinessException("No available seats for course: 10"))
                .when(courseService).incrementEnrolledCount(10L);

        enrollmentEventListener.handleEnrollmentApproved(event);

        verify(processedEventRepository).save(argThat(pe ->
                pe.getEventId().equals("evt-biz") && pe.getEventType().equals("EnrollmentApprovedEvent")));
    }

    @Test
    void shouldPropagateGenericExceptionOnApproved() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("evt-err")
                .courseId(10L)
                .employeeId(1L)
                .enrollmentId(100L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-err")).thenReturn(false);
        doThrow(new RuntimeException("DB connection lost"))
                .when(courseService).incrementEnrolledCount(10L);

        assertThrows(RuntimeException.class,
                () -> enrollmentEventListener.handleEnrollmentApproved(event));

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldProcessEnrollmentCancelledEvent() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("evt-cancel-1")
                .courseId(20L)
                .employeeId(2L)
                .enrollmentId(200L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-cancel-1")).thenReturn(false);

        enrollmentEventListener.handleEnrollmentCancelled(event);

        verify(courseService).decrementEnrolledCount(20L);
        verify(processedEventRepository).save(argThat(pe ->
                pe.getEventId().equals("evt-cancel-1") && pe.getEventType().equals("EnrollmentCancelledEvent")));
    }

    @Test
    void shouldSkipAlreadyProcessedCancelledEvent() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("evt-cancel-dup")
                .courseId(20L)
                .employeeId(2L)
                .enrollmentId(200L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-cancel-dup")).thenReturn(true);

        enrollmentEventListener.handleEnrollmentCancelled(event);

        verify(courseService, never()).decrementEnrolledCount(anyLong());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldHandleBusinessExceptionOnCancelled() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("evt-cancel-biz")
                .courseId(20L)
                .employeeId(2L)
                .enrollmentId(200L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-cancel-biz")).thenReturn(false);
        doThrow(new BusinessException("Cannot decrement enrolled count for course: 20. Count is already 0."))
                .when(courseService).decrementEnrolledCount(20L);

        enrollmentEventListener.handleEnrollmentCancelled(event);

        verify(processedEventRepository).save(argThat(pe ->
                pe.getEventId().equals("evt-cancel-biz") && pe.getEventType().equals("EnrollmentCancelledEvent")));
    }

    @Test
    void shouldPropagateGenericExceptionOnCancelled() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("evt-cancel-err")
                .courseId(20L)
                .employeeId(2L)
                .enrollmentId(200L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-cancel-err")).thenReturn(false);
        doThrow(new RuntimeException("Timeout"))
                .when(courseService).decrementEnrolledCount(20L);

        assertThrows(RuntimeException.class,
                () -> enrollmentEventListener.handleEnrollmentCancelled(event));

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldHandleDataIntegrityViolationOnApproved() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("evt-di")
                .courseId(10L)
                .employeeId(1L)
                .enrollmentId(100L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-di")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Duplicate"))
                .when(processedEventRepository).save(any());

        enrollmentEventListener.handleEnrollmentApproved(event);

        verify(courseService).incrementEnrolledCount(10L);
    }

    @Test
    void shouldHandleDataIntegrityViolationOnCancelled() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("evt-cancel-di")
                .courseId(20L)
                .employeeId(2L)
                .enrollmentId(200L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-cancel-di")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Duplicate"))
                .when(processedEventRepository).save(any());

        enrollmentEventListener.handleEnrollmentCancelled(event);

        verify(courseService).decrementEnrolledCount(20L);
    }

    @Test
    void shouldHandleBusinessExceptionThenDataIntegrityViolationOnApproved() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("evt-biz-di")
                .courseId(10L)
                .employeeId(1L)
                .enrollmentId(100L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-biz-di")).thenReturn(false);
        doThrow(new BusinessException("No available seats"))
                .when(courseService).incrementEnrolledCount(10L);
        doThrow(new DataIntegrityViolationException("Duplicate"))
                .when(processedEventRepository).save(any());

        enrollmentEventListener.handleEnrollmentApproved(event);

        verify(processedEventRepository, times(1)).save(argThat(pe ->
                pe.getEventId().equals("evt-biz-di") && pe.getEventType().equals("EnrollmentApprovedEvent")));
    }

    @Test
    void shouldHandleBusinessExceptionThenDataIntegrityViolationOnCancelled() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("evt-cancel-biz-di")
                .courseId(20L)
                .employeeId(2L)
                .enrollmentId(200L)
                .source("enrollment-service")
                .timestamp(LocalDateTime.now())
                .build();

        when(processedEventRepository.existsById("evt-cancel-biz-di")).thenReturn(false);
        doThrow(new BusinessException("Cannot decrement count"))
                .when(courseService).decrementEnrolledCount(20L);
        doThrow(new DataIntegrityViolationException("Duplicate"))
                .when(processedEventRepository).save(any());

        enrollmentEventListener.handleEnrollmentCancelled(event);

        verify(processedEventRepository, times(1)).save(argThat(pe ->
                pe.getEventId().equals("evt-cancel-biz-di") && pe.getEventType().equals("EnrollmentCancelledEvent")));
    }
}

package com.poc.training.notification.service;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.model.ProcessedEvent;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.notification.repository.ProcessedEventRepository;
import com.poc.training.notification.sender.NotificationSender;
import com.poc.training.shared.event.*;
import com.poc.training.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, notificationSender, processedEventRepository);
    }

    @Test
    void shouldFindNotificationsByRecipientId() {
        Notification n1 = Notification.builder().id(1L).recipientId(1L).type(NotificationType.ENROLLMENT_CREATED).status(NotificationStatus.SENT).build();
        Notification n2 = Notification.builder().id(2L).recipientId(1L).type(NotificationType.CERTIFICATE_GENERATED).status(NotificationStatus.SENT).build();
        when(notificationRepository.findByRecipientId(eq(1L), any(PageRequest.class))).thenReturn(new PageImpl<>(Arrays.asList(n1, n2)));

        var result = notificationService.findByRecipientId(1L, PageRequest.of(0, 10));
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldFindNotificationById() {
        Notification n = Notification.builder().id(1L).recipientId(1L).message("Test").type(NotificationType.ENROLLMENT_CREATED).status(NotificationStatus.SENT).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        NotificationResponse result = notificationService.findById(1L);
        assertNotNull(result);
        assertEquals("Test", result.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNotificationNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.findById(99L));
    }

    @Test
    void shouldHandleEnrollmentCreatedEvent() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationSender.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });
        notificationService.handleEnrollmentCreated(event);

        verify(notificationSender).send(any(Notification.class));
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getType() == NotificationType.ENROLLMENT_CREATED &&
                n.getRecipientId().equals(1L)
        ));
    }

    @Test
    void shouldHandleEnrollmentApprovedEvent() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationSender.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });
        notificationService.handleEnrollmentApproved(event);

        verify(notificationSender).send(any(Notification.class));
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getType() == NotificationType.ENROLLMENT_APPROVED &&
                n.getRecipientId().equals(1L)
        ));
    }

    @Test
    void shouldHandleCertificateGeneratedEvent() {
        CertificateGeneratedEvent event = CertificateGeneratedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("certification-service")
                .certificateId(1L)
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-12345")
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationSender.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });
        notificationService.handleCertificateGenerated(event);

        verify(notificationSender).send(any(Notification.class));
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getType() == NotificationType.CERTIFICATE_GENERATED &&
                n.getRecipientId().equals(1L)
        ));
    }

    @Test
    void shouldThrowWhenSenderReturnsFalse() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationSender.send(any(Notification.class))).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        assertThrows(RuntimeException.class, () ->
            notificationService.handleEnrollmentCreated(event)
        );
    }

    @Test
    void shouldThrowWhenSenderThrowsException() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationSender.send(any(Notification.class))).thenThrow(new RuntimeException("Send failed"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        assertThrows(RuntimeException.class, () ->
            notificationService.handleEnrollmentCreated(event)
        );
    }

    @Test
    void shouldHandleCourseCompletedEvent() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationSender.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });
        notificationService.handleCourseCompleted(event);

        verify(notificationSender).send(any(Notification.class));
        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getType() == NotificationType.COURSE_COMPLETED &&
                n.getRecipientId().equals(1L)
        ));
    }

    @Test
    void shouldSkipEnrollmentApprovedWhenAlreadyProcessed() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationService.handleEnrollmentApproved(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSkipCertificateGeneratedWhenAlreadyProcessed() {
        CertificateGeneratedEvent event = CertificateGeneratedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("certification-service")
                .certificateId(1L)
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-12345")
                .build();

        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationService.handleCertificateGenerated(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSkipEnrollmentRejectedWhenAlreadyProcessed() {
        EnrollmentRejectedEvent event = EnrollmentRejectedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationService.handleEnrollmentRejected(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSkipEnrollmentCancelledWhenAlreadyProcessed() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationService.handleEnrollmentCancelled(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSkipCourseCompletedWhenAlreadyProcessed() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationService.handleCourseCompleted(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSkipNotificationOnDataIntegrityViolation() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(processedEventRepository).save(any(ProcessedEvent.class));

        notificationService.handleEnrollmentCreated(event);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationSender, never()).send(any(Notification.class));
    }

    @Test
    void shouldSkipWhenNotificationAlreadyExists() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(anyString())).thenReturn(false);
        when(notificationRepository.existsByReferenceIdAndReferenceTypeAndType(
                anyString(), anyString(), any(NotificationType.class))).thenReturn(true);

        notificationService.handleEnrollmentCreated(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(argThat(n -> n instanceof Notification));
    }

    @Test
    void shouldHandleEnrollmentCreatedWhenAlreadyProcessed() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        notificationService.handleEnrollmentCreated(event);

        verify(notificationSender, never()).send(any(Notification.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}

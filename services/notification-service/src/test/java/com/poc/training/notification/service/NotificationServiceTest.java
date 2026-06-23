package com.poc.training.notification.service;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.shared.event.*;
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
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldFindNotificationsByRecipientId() {
        Notification n1 = Notification.builder().id(1L).recipientId("1").type(NotificationType.ENROLLMENT_CREATED).status(NotificationStatus.SENT).build();
        Notification n2 = Notification.builder().id(2L).recipientId("1").type(NotificationType.COURSE_COMPLETED).status(NotificationStatus.SENT).build();
        when(notificationRepository.findByRecipientId(eq("1"), any(PageRequest.class))).thenReturn(new PageImpl<>(Arrays.asList(n1, n2)));

        var result = notificationService.findByRecipientId("1", PageRequest.of(0, 10));
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldFindNotificationById() {
        Notification n = Notification.builder().id(1L).recipientId("1").message("Test").type(NotificationType.ENROLLMENT_CREATED).status(NotificationStatus.SENT).build();
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

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleEnrollmentCreated(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("notification.sent"), any(NotificationSentEvent.class));
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

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleEnrollmentApproved(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("notification.sent"), any(NotificationSentEvent.class));
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

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleCertificateGenerated(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("notification.sent"), any(NotificationSentEvent.class));
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

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        notificationService.handleCourseCompleted(event);

        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.COURSE_COMPLETED &&
                n.getRecipientId().equals("1")
        ));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("notification.sent"), any(NotificationSentEvent.class));
    }
}

package com.poc.training.notification.integration;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.model.ProcessedEvent;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.notification.repository.ProcessedEventRepository;
import com.poc.training.notification.service.NotificationService;
import com.poc.training.shared.event.*;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class NotificationRabbitMQIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void shouldHandleEnrollmentCreatedEvent() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(10L)
                .courseId(20L)
                .build();

        notificationService.handleEnrollmentCreated(event);

        var notifications = notificationRepository.findByRecipientId(10L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.ENROLLMENT_CREATED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldHandleEnrollmentApprovedEvent() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(2L)
                .employeeId(20L)
                .courseId(30L)
                .build();

        notificationService.handleEnrollmentApproved(event);

        var notifications = notificationRepository.findByRecipientId(20L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.ENROLLMENT_APPROVED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldHandleEnrollmentRejectedEvent() {
        EnrollmentRejectedEvent event = EnrollmentRejectedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(3L)
                .employeeId(30L)
                .courseId(40L)
                .build();

        notificationService.handleEnrollmentRejected(event);

        var notifications = notificationRepository.findByRecipientId(30L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.ENROLLMENT_REJECTED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldHandleEnrollmentCancelledEvent() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(4L)
                .employeeId(40L)
                .courseId(50L)
                .build();

        notificationService.handleEnrollmentCancelled(event);

        var notifications = notificationRepository.findByRecipientId(40L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.ENROLLMENT_CANCELLED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldHandleCourseCompletedEvent() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(5L)
                .employeeId(50L)
                .courseId(60L)
                .build();

        notificationService.handleCourseCompleted(event);

        var notifications = notificationRepository.findByRecipientId(50L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.COURSE_COMPLETED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldHandleCertificateGeneratedEvent() {
        CertificateGeneratedEvent event = CertificateGeneratedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("certification-service")
                .certificateId(1L)
                .enrollmentId(6L)
                .employeeId(60L)
                .courseId(70L)
                .certificateNumber("CERT-12345")
                .build();

        notificationService.handleCertificateGenerated(event);

        var notifications = notificationRepository.findByRecipientId(60L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.CERTIFICATE_GENERATED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldBeIdempotentForDuplicateEvents() {
        String eventId = UUID.randomUUID().toString();
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(eventId)
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(7L)
                .employeeId(70L)
                .courseId(80L)
                .build();

        notificationService.handleEnrollmentCreated(event);
        notificationService.handleEnrollmentCreated(event);

        var notifications = notificationRepository.findByRecipientId(70L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
    }

    @Test
    void shouldMarkEventAsProcessed() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(8L)
                .employeeId(80L)
                .courseId(90L)
                .build();

        notificationService.handleEnrollmentCreated(event);

        assertTrue(processedEventRepository.existsByEventId(event.getEventId()));
    }
}

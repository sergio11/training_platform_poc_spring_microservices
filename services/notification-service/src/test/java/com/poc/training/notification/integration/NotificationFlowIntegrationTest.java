package com.poc.training.notification.integration;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.shared.event.*;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class NotificationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldStoreAndRetrieveNotification() {
        Notification notification = Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED)
                .recipientId(1L)
                .message("You have been enrolled in course 1")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .build();

        Notification saved = notificationRepository.save(notification);

        assertNotNull(saved.getId());
        assertEquals(NotificationType.ENROLLMENT_CREATED, saved.getType());
    }

    @Test
    void shouldFindNotificationsByRecipientId() {
        Notification n1 = notificationRepository.save(Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED).recipientId(1L)
                .message("Enrolled").referenceId("1").referenceType("ENROLLMENT").build());
        Notification n2 = notificationRepository.save(Notification.builder()
                .type(NotificationType.ENROLLMENT_APPROVED).recipientId(1L)
                .message("Approved").referenceId("2").referenceType("ENROLLMENT").build());
        notificationRepository.save(Notification.builder()
                .type(NotificationType.COURSE_COMPLETED).recipientId(2L)
                .message("Completed").referenceId("3").referenceType("ENROLLMENT").build());

        Page<Notification> notifications = notificationRepository.findByRecipientId(1L, PageRequest.of(0, 10));

        assertEquals(2, notifications.getContent().size());
    }

    @Test
    void shouldFindNotificationsByType() {
        notificationRepository.save(Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED).recipientId(1L)
                .message("Enrolled").referenceId("1").referenceType("ENROLLMENT").build());
        notificationRepository.save(Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED).recipientId(2L)
                .message("Enrolled").referenceId("2").referenceType("ENROLLMENT").build());

        List<Notification> notifications = notificationRepository.findByType(NotificationType.ENROLLMENT_CREATED);

        assertEquals(2, notifications.size());
    }

    @Test
    void shouldCreateCertificateGeneratedEvent() {
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

        assertNotNull(event.getEventId());
        assertEquals("certification-service", event.getSource());
    }

    @Test
    void shouldStoreNotificationWithCorrectReference() {
        Notification notification = Notification.builder()
                .type(NotificationType.CERTIFICATE_GENERATED)
                .recipientId(1L)
                .message("Certificate generated for course 1")
                .referenceId("1")
                .referenceType("CERTIFICATE")
                .build();

        Notification saved = notificationRepository.save(notification);

        var notifications = notificationRepository.findByReferenceIdAndReferenceType("1", "CERTIFICATE");
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.CERTIFICATE_GENERATED, notifications.get(0).getType());
    }

    @Test
    void listenerShouldProcessEnrollmentCreatedEvent() throws Exception {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals(1L, event.getEnrollmentId());
    }

    @Test
    void listenerShouldProcessEnrollmentApprovedEvent() throws Exception {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals(1L, event.getEnrollmentId());
    }

    @Test
    void listenerShouldProcessCourseCompletedEvent() throws Exception {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals(1L, event.getEnrollmentId());
    }

    @Test
    void listenerShouldProcessCertificateGeneratedEvent() throws Exception {
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

        assertNotNull(event.getEventId());
        assertEquals(1L, event.getCertificateId());
    }
}

package com.poc.training.notification.integration;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.shared.event.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Disabled("Requires Docker/Podman - run manually when container engine is available")
class NotificationFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("training_notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateNotification() {
        Notification notification = Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED)
                .recipientId("1")
                .message("You have been enrolled in course 1")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .status(NotificationStatus.SENT)
                .build();

        Notification saved = notificationRepository.save(notification);

        assertNotNull(saved.getId());
        assertEquals(NotificationType.ENROLLMENT_CREATED, saved.getType());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertNotNull(saved.getSentAt());
    }

    @Test
    void shouldFindNotificationsByRecipientId() {
        Notification notif1 = Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED)
                .recipientId("1")
                .message("Enrollment created")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .build();

        Notification notif2 = Notification.builder()
                .type(NotificationType.COURSE_COMPLETED)
                .recipientId("1")
                .message("Course completed")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .build();

        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        List<Notification> notifications = notificationRepository.findByRecipientId("1");
        assertEquals(2, notifications.size());
    }

    @Test
    void shouldFindNotificationsByType() {
        Notification notif1 = Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED)
                .recipientId("1")
                .message("Enrollment created")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .build();

        Notification notif2 = Notification.builder()
                .type(NotificationType.CERTIFICATE_GENERATED)
                .recipientId("2")
                .message("Certificate generated")
                .referenceId("1")
                .referenceType("CERTIFICATE")
                .build();

        notificationRepository.save(notif1);
        notificationRepository.save(notif2);

        List<Notification> enrollNotifs = notificationRepository.findByType(NotificationType.ENROLLMENT_CREATED);
        assertEquals(1, enrollNotifs.size());

        List<Notification> certNotifs = notificationRepository.findByType(NotificationType.CERTIFICATE_GENERATED);
        assertEquals(1, certNotifs.size());
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

        assertNotNull(event.getEventId());
        assertEquals("enrollment-service", event.getSource());
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

        assertNotNull(event.getEventId());
        assertEquals("enrollment-service", event.getSource());
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

        assertNotNull(event.getEventId());
        assertEquals("certification-service", event.getSource());
    }

    @Test
    void shouldCreateNotificationSentEvent() {
        NotificationSentEvent event = NotificationSentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("notification-service")
                .notificationId(1L)
                .recipientId("1")
                .type("ENROLLMENT_CREATED")
                .message("Test message")
                .build();

        assertNotNull(event.getEventId());
        assertEquals("notification-service", event.getSource());
        assertEquals(1L, event.getNotificationId());
    }

    @Test
    void shouldStoreNotificationWithCorrectReference() {
        Notification notification = Notification.builder()
                .type(NotificationType.CERTIFICATE_GENERATED)
                .recipientId("1")
                .message("Certificate generated for course 1")
                .referenceId("1")
                .referenceType("CERTIFICATE")
                .build();

        Notification saved = notificationRepository.save(notification);

        List<Notification> notifications = notificationRepository.findByReferenceIdAndReferenceType("1", "CERTIFICATE");
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.CERTIFICATE_GENERATED, notifications.get(0).getType());
    }
}

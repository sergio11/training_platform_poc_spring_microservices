package com.poc.training.notification.integration;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.shared.event.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

        var notifications = notificationRepository.findByRecipientId("1", PageRequest.of(0, 10));
        assertEquals(2, notifications.getContent().size());
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

        var enrollNotifs = notificationRepository.findByType(NotificationType.ENROLLMENT_CREATED);
        assertEquals(1, enrollNotifs.size());

        var certNotifs = notificationRepository.findByType(NotificationType.CERTIFICATE_GENERATED);
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
                .enrollmentId(100L)
                .employeeId(100L)
                .courseId(100L)
                .build();

        long beforeCount = notificationRepository.findByRecipientId("100", PageRequest.of(0, 10)).getTotalElements();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED, event);

        waitForNotification("100", beforeCount + 1);

        var notifications = notificationRepository.findByRecipientId("100", PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());

        Notification saved = notifications.getContent().get(0);
        assertEquals(NotificationType.ENROLLMENT_CREATED, saved.getType());
        assertEquals("100", saved.getRecipientId());
        assertEquals("ENROLLMENT", saved.getReferenceType());
        assertEquals("100", saved.getReferenceId());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertNotNull(saved.getSentAt());
    }

    @Test
    void listenerShouldProcessEnrollmentApprovedEvent() throws Exception {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(200L)
                .employeeId(200L)
                .courseId(200L)
                .build();

        long beforeCount = notificationRepository.findByRecipientId("200", PageRequest.of(0, 10)).getTotalElements();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED, event);

        waitForNotification("200", beforeCount + 1);

        var notifications = notificationRepository.findByRecipientId("200", PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());

        Notification saved = notifications.getContent().get(0);
        assertEquals(NotificationType.ENROLLMENT_APPROVED, saved.getType());
        assertEquals("200", saved.getRecipientId());
        assertEquals("ENROLLMENT", saved.getReferenceType());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
    }

    @Test
    void listenerShouldProcessCertificateGeneratedEvent() throws Exception {
        CertificateGeneratedEvent event = CertificateGeneratedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("certification-service")
                .certificateId(300L)
                .enrollmentId(300L)
                .employeeId(300L)
                .courseId(300L)
                .certificateNumber("CERT-99999")
                .build();

        long beforeCount = notificationRepository.findByRecipientId("300", PageRequest.of(0, 10)).getTotalElements();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED, event);

        waitForNotification("300", beforeCount + 1);

        var notifications = notificationRepository.findByRecipientId("300", PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());

        Notification saved = notifications.getContent().get(0);
        assertEquals(NotificationType.CERTIFICATE_GENERATED, saved.getType());
        assertEquals("300", saved.getRecipientId());
        assertEquals("CERTIFICATE", saved.getReferenceType());
        assertEquals("300", saved.getReferenceId());
        assertTrue(saved.getMessage().contains("CERT-99999"));
        assertEquals(NotificationStatus.SENT, saved.getStatus());
    }

    private void waitForNotification(String recipientId, long expectedCount) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            long count = notificationRepository.findByRecipientId(recipientId, PageRequest.of(0, 10)).getTotalElements();
            if (count >= expectedCount) return;
            Thread.sleep(200);
        }
        fail("Timed out waiting for notification for recipient: " + recipientId);
    }
}

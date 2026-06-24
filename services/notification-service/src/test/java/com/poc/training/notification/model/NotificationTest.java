package com.poc.training.notification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void shouldSetDefaultValuesOnPrePersist() {
        Notification notification = Notification.builder()
                .type(NotificationType.ENROLLMENT_CREATED)
                .recipientId("1")
                .message("Test message")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .build();

        notification.onCreate();

        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertNotNull(notification.getSentAt());
    }

    @Test
    void shouldCreateNotificationWithBuilder() {
        Notification notification = Notification.builder()
                .id(1L)
                .type(NotificationType.CERTIFICATE_GENERATED)
                .recipientId("1")
                .message("Certificate generated")
                .referenceId("1")
                .referenceType("CERTIFICATE")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        assertEquals(1L, notification.getId());
        assertEquals(NotificationType.CERTIFICATE_GENERATED, notification.getType());
        assertEquals("1", notification.getRecipientId());
        assertEquals("Certificate generated", notification.getMessage());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        Notification notification = new Notification();
        assertNotNull(notification);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        Notification notification = new Notification(1L, NotificationType.ENROLLMENT_CREATED, "1", "Msg", "1", "ENROLLMENT", LocalDateTime.now(), NotificationStatus.SENT);
        assertEquals(NotificationType.ENROLLMENT_CREATED, notification.getType());
    }
}

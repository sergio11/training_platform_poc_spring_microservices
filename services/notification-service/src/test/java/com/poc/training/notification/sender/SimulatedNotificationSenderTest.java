package com.poc.training.notification.sender;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedNotificationSenderTest {

    private final SimulatedNotificationSender sender = new SimulatedNotificationSender();

    @Test
    void shouldReturnTrueOnSend() {
        Notification notification = Notification.builder()
                .id(1L)
                .type(NotificationType.ENROLLMENT_CREATED)
                .recipientId(42L)
                .message("Test message")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .status(NotificationStatus.PENDING)
                .build();

        boolean result = sender.send(notification);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForDifferentNotificationTypes() {
        for (NotificationType type : NotificationType.values()) {
            Notification notification = Notification.builder()
                    .id(1L)
                    .type(type)
                    .recipientId(1L)
                    .message("Test")
                    .referenceId("1")
                    .referenceType("TEST")
                    .status(NotificationStatus.PENDING)
                    .build();

            assertThat(sender.send(notification)).isTrue();
        }
    }
}

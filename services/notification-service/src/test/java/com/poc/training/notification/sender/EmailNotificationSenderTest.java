package com.poc.training.notification.sender;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationSender emailNotificationSender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationSender, "fromAddress", "noreply@training-poc.com");
        ReflectionTestUtils.setField(emailNotificationSender, "recipientOverride", "");
    }

    private Notification buildNotification(Long recipientId, NotificationType type, String message) {
        return Notification.builder()
                .id(1L)
                .recipientId(recipientId)
                .type(type)
                .message(message)
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .status(NotificationStatus.PENDING)
                .build();
    }

    @Test
    void shouldSendEmailWithDefaultRecipient() {
        Notification notification = buildNotification(42L, NotificationType.ENROLLMENT_CREATED, "You have been enrolled");

        boolean result = emailNotificationSender.send(notification);

        assertTrue(result);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("42@training.local", sent.getTo()[0]);
        assertEquals("noreply@training-poc.com", sent.getFrom());
        assertEquals("Training Platform - ENROLLMENT_CREATED", sent.getSubject());
        assertEquals("You have been enrolled", sent.getText());
    }

    @Test
    void shouldSendEmailWithOverrideRecipient() {
        ReflectionTestUtils.setField(emailNotificationSender, "recipientOverride", "admin@company.com");
        Notification notification = buildNotification(42L, NotificationType.CERTIFICATE_GENERATED, "Certificate ready");

        boolean result = emailNotificationSender.send(notification);

        assertTrue(result);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("admin@company.com", captor.getValue().getTo()[0]);
    }

    @Test
    void shouldReturnFalseOnSendFailure() {
        Notification notification = buildNotification(42L, NotificationType.ENROLLMENT_CREATED, "Test message");
        doThrow(new RuntimeException("Mail server unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        boolean result = emailNotificationSender.send(notification);

        assertFalse(result);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}

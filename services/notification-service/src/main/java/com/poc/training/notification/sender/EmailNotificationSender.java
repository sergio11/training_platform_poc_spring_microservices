package com.poc.training.notification.sender;

import com.poc.training.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("email")
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${app.notification.email.from:noreply@training-poc.com}")
    private String fromAddress;

    @Value("${app.notification.email.recipient-override:}")
    private String recipientOverride;

    @Override
    public boolean send(Notification notification) {
        try {
            String to = recipientOverride.isBlank() ? notification.getRecipientId() + "@training.local" : recipientOverride;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Training Platform - " + notification.getType().name());
            message.setText(notification.getMessage());

            mailSender.send(message);
            log.info("[EMAIL {}] Sent to: {}, Subject: {}", notification.getType(), to, message.getSubject());
            return true;
        } catch (Exception e) {
            log.error("[EMAIL {}] Failed to send to {}: {}", notification.getType(), notification.getRecipientId(), e.getMessage());
            return false;
        }
    }
}

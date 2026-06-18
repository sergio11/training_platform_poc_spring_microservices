package com.poc.training.notification.service;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.shared.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "training.exchange";

    public List<Notification> findByRecipientId(String recipientId) {
        return notificationRepository.findByRecipientId(recipientId);
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    @RabbitListener(queues = "enrollment.created")
    public void handleEnrollmentCreated(EnrollmentCreatedEvent event) {
        log.info("Received EnrollmentCreatedEvent for enrollment: {}", event.getEnrollmentId());

        String message = String.format(
                "You have been enrolled in course %d. Your enrollment ID is %d.",
                event.getCourseId(), event.getEnrollmentId());

        saveAndPublishNotification(
                NotificationType.ENROLLMENT_CREATED,
                String.valueOf(event.getEmployeeId()),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT"
        );
    }

    @RabbitListener(queues = "enrollment.approved")
    public void handleEnrollmentApproved(EnrollmentApprovedEvent event) {
        log.info("Received EnrollmentApprovedEvent for enrollment: {}", event.getEnrollmentId());

        String message = String.format(
                "Your enrollment %d for course %d has been approved.",
                event.getEnrollmentId(), event.getCourseId());

        saveAndPublishNotification(
                NotificationType.ENROLLMENT_APPROVED,
                String.valueOf(event.getEmployeeId()),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT"
        );
    }

    @RabbitListener(queues = "certificate.generated")
    public void handleCertificateGenerated(CertificateGeneratedEvent event) {
        log.info("Received CertificateGeneratedEvent for certificate: {}", event.getCertificateId());

        String message = String.format(
                "Congratulations! Your certificate %s has been generated for course %d.",
                event.getCertificateNumber(), event.getCourseId());

        saveAndPublishNotification(
                NotificationType.CERTIFICATE_GENERATED,
                String.valueOf(event.getEmployeeId()),
                message,
                String.valueOf(event.getCertificateId()),
                "CERTIFICATE"
        );
    }

    private void saveAndPublishNotification(NotificationType type, String recipientId, String message,
                                            String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .type(type)
                .recipientId(recipientId)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .status(NotificationStatus.SENT)
                .build();

        Notification saved = notificationRepository.save(notification);

        NotificationSentEvent event = NotificationSentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("notification-service")
                .notificationId(saved.getId())
                .recipientId(recipientId)
                .type(type.name())
                .message(message)
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "notification.sent", event);
        log.info("Published NotificationSentEvent for notification: {}", saved.getId());
    }
}

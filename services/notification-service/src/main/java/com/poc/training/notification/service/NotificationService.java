package com.poc.training.notification.service;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.shared.event.*;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .recipientId(notification.getRecipientId())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .sentAt(notification.getSentAt())
                .status(notification.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findByRecipientId(String recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientId(recipientId, pageable).map(NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return toResponse(notification);
    }

    @Transactional
    @RabbitListener(queues = "enrollment.created")
    public void handleEnrollmentCreated(EnrollmentCreatedEvent event) {
        log.info("Received EnrollmentCreatedEvent for enrollment: {}", event.getEnrollmentId());

        String message = String.format(
                "You have been enrolled in course %d. Your enrollment ID is %d.",
                event.getCourseId(), event.getEnrollmentId());

        saveNotification(
                NotificationType.ENROLLMENT_CREATED,
                String.valueOf(event.getEmployeeId()),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT"
        );
    }

    @Transactional
    @RabbitListener(queues = "enrollment.approved")
    public void handleEnrollmentApproved(EnrollmentApprovedEvent event) {
        log.info("Received EnrollmentApprovedEvent for enrollment: {}", event.getEnrollmentId());

        String message = String.format(
                "Your enrollment %d for course %d has been approved.",
                event.getEnrollmentId(), event.getCourseId());

        saveNotification(
                NotificationType.ENROLLMENT_APPROVED,
                String.valueOf(event.getEmployeeId()),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT"
        );
    }

    @Transactional
    @RabbitListener(queues = "certificate.generated")
    public void handleCertificateGenerated(CertificateGeneratedEvent event) {
        log.info("Received CertificateGeneratedEvent for certificate: {}", event.getCertificateId());

        String message = String.format(
                "Congratulations! Your certificate %s has been generated for course %d.",
                event.getCertificateNumber(), event.getCourseId());

        saveNotification(
                NotificationType.CERTIFICATE_GENERATED,
                String.valueOf(event.getEmployeeId()),
                message,
                String.valueOf(event.getCertificateId()),
                "CERTIFICATE"
        );
    }

    private void saveNotification(NotificationType type, String recipientId, String message,
                                  String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .type(type)
                .recipientId(recipientId)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .status(NotificationStatus.SENT)
                .build();

        notificationRepository.save(notification);
        log.info("Saved notification of type {} for recipient: {}", type, recipientId);
    }
}

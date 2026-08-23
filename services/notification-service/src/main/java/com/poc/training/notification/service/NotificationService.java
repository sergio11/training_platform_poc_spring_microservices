package com.poc.training.notification.service;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.model.ProcessedEvent;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.notification.repository.ProcessedEventRepository;
import com.poc.training.notification.sender.NotificationSender;
import com.poc.training.shared.event.*;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final ProcessedEventRepository processedEventRepository;

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
    public Page<NotificationResponse> findByRecipientId(Long recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientId(recipientId, pageable).map(NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return toResponse(notification);
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_ENROLLMENT_CREATED)
    public void handleEnrollmentCreated(EnrollmentCreatedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "You have been enrolled in course %d. Your enrollment ID is %d.",
                event.getCourseId(), event.getEnrollmentId());

        saveNotification(
                NotificationType.ENROLLMENT_CREATED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_ENROLLMENT_APPROVED)
    public void handleEnrollmentApproved(EnrollmentApprovedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "Your enrollment %d for course %d has been approved.",
                event.getEnrollmentId(), event.getCourseId());

        saveNotification(
                NotificationType.ENROLLMENT_APPROVED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_CERTIFICATE_GENERATED)
    public void handleCertificateGenerated(CertificateGeneratedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "Congratulations! Your certificate %s has been generated for course %d.",
                event.getCertificateNumber(), event.getCourseId());

        saveNotification(
                NotificationType.CERTIFICATE_GENERATED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getCertificateId()),
                "CERTIFICATE",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_ENROLLMENT_REJECTED)
    public void handleEnrollmentRejected(EnrollmentRejectedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "Your enrollment %d for course %d has been rejected.",
                event.getEnrollmentId(), event.getCourseId());

        saveNotification(
                NotificationType.ENROLLMENT_REJECTED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_ENROLLMENT_CANCELLED)
    public void handleEnrollmentCancelled(EnrollmentCancelledEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "Your enrollment %d for course %d has been cancelled.",
                event.getEnrollmentId(), event.getCourseId());

        saveNotification(
                NotificationType.ENROLLMENT_CANCELLED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_COURSE_COMPLETED)
    public void handleCourseCompleted(CourseCompletedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "You have completed course %d. Your enrollment ID is %d.",
                event.getCourseId(), event.getEnrollmentId());

        saveNotification(
                NotificationType.COURSE_COMPLETED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEnrollmentId()),
                "ENROLLMENT",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_EMPLOYEE_UPDATED)
    public void handleEmployeeUpdated(EmployeeUpdatedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "Your employee profile has been updated. Name: %s, Department: %s, Position: %s.",
                event.getFullName(), event.getDepartment(), event.getPosition());

        saveNotification(
                NotificationType.EMPLOYEE_UPDATED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEmployeeId()),
                "EMPLOYEE",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_EMPLOYEE_CREATED)
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = String.format(
                "Welcome %s! Your employee profile has been created. Department: %s.",
                event.getFullName(), event.getDepartment());

        saveNotification(
                NotificationType.EMPLOYEE_CREATED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEmployeeId()),
                "EMPLOYEE",
                event.getEventId()
        );
    }

    @Transactional
    @RabbitListener(queues = com.poc.training.notification.config.RabbitMQConfig.QUEUE_EMPLOYEE_DEACTIVATED)
    public void handleEmployeeDeactivated(EmployeeDeactivatedEvent event) {
        if (isAlreadyProcessed(event.getEventId())) return;

        String message = "Your employee profile has been deactivated.";

        saveNotification(
                NotificationType.EMPLOYEE_DEACTIVATED,
                event.getEmployeeId(),
                message,
                String.valueOf(event.getEmployeeId()),
                "EMPLOYEE",
                event.getEventId()
        );
    }

    private boolean isAlreadyProcessed(String eventId) {
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Event already processed, skipping: {}", eventId);
            return true;
        }
        return false;
    }

    private void saveNotification(NotificationType type, Long recipientId, String message,
                                  String referenceId, String referenceType, String eventId) {
        try {
            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(type.name())
                    .build();
            processedEventRepository.save(processed);
        } catch (DataIntegrityViolationException e) {
            log.info("Event already processed (concurrent), skipping: {}", eventId);
            return;
        }

        if (notificationRepository.existsByReferenceIdAndReferenceTypeAndType(referenceId, referenceType, type)) {
            log.info("Notification already exists for reference {}/{} type {}, skipping",
                    referenceId, referenceType, type);
            return;
        }

        Notification notification = Notification.builder()
                .type(type)
                .recipientId(recipientId)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .status(NotificationStatus.PENDING)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Saved notification of type {} for recipient: {}", type, recipientId);

        try {
            boolean sent = notificationSender.send(saved);
            if (!sent) {
                throw new RuntimeException("Send returned false for notification " + saved.getId());
            }
            saved.setStatus(NotificationStatus.DELIVERED);
            notificationRepository.save(saved);
            log.info("Delivered notification {} of type {} for recipient: {}", saved.getId(), type, recipientId);
        } catch (Exception e) {
            log.error("Failed to process notification {}: {}", saved.getId(), e.getMessage());
            throw e;
        }
    }
}

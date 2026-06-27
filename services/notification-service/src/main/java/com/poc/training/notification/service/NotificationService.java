package com.poc.training.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.model.OutboxEvent;
import com.poc.training.notification.model.ProcessedEvent;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.notification.repository.OutboxEventRepository;
import com.poc.training.notification.repository.ProcessedEventRepository;
import com.poc.training.notification.sender.NotificationSender;
import com.poc.training.shared.event.*;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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
    @RabbitListener(queues = "enrollment.created")
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
    @RabbitListener(queues = "enrollment.approved")
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
    @RabbitListener(queues = "certificate.generated")
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
    @RabbitListener(queues = "enrollment.rejected")
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
    @RabbitListener(queues = "course.completed")
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

        boolean sent = false;
        try {
            sent = notificationSender.send(saved);
            saved.setStatus(sent ? NotificationStatus.DELIVERED : NotificationStatus.FAILED);
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", saved.getId(), e.getMessage());
            saved.setStatus(NotificationStatus.FAILED);
        }
        notificationRepository.save(saved);

        if (sent) {
            NotificationSentEvent sentEvent = NotificationSentEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .source("notification-service")
                    .notificationId(saved.getId())
                    .recipientId(recipientId)
                    .notificationType(type.name())
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();

            persistOutboxEvent("Notification", String.valueOf(saved.getId()), "NotificationSentEvent",
                    sentEvent, RabbitMQConstants.ROUTING_KEY_NOTIFICATION_SENT);
            log.info("Persisted NotificationSentEvent to outbox for notification: {}", saved.getId());
        }
    }

    private void persistOutboxEvent(String aggregateType, String aggregateId, String eventType,
                                    Object event, String routingKey) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(event))
                    .routingKey(routingKey)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} for outbox: {}", eventType, e.getMessage());
        }
    }
}

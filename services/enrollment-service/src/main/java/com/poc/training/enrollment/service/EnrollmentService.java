package com.poc.training.enrollment.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.poc.training.shared.dto.CourseDTO;
import com.poc.training.enrollment.client.EmployeeClient;
import com.poc.training.enrollment.client.TrainingCatalogClient;
import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.model.OutboxEvent;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.enrollment.repository.OutboxEventRepository;
import com.poc.training.shared.event.EnrollmentCancelledEvent;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import com.poc.training.shared.event.EnrollmentCreatedEvent;
import com.poc.training.shared.event.EnrollmentRejectedEvent;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TrainingCatalogClient trainingCatalogClient;
    private final EmployeeClient employeeClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public static EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .employeeId(enrollment.getEmployeeId())
                .courseId(enrollment.getCourseId())
                .status(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findByEmployeeId(Long employeeId, Pageable pageable) {
        return enrollmentRepository.findByEmployeeId(employeeId, pageable).map(EnrollmentService::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse enroll(Long employeeId, Long courseId) {
        log.info("Processing enrollment for employee {} in course {}", employeeId, courseId);

        if (!employeeClient.existsById(employeeId)) {
            throw new BusinessException("Employee not found with id: " + employeeId);
        }

        CourseDTO course = trainingCatalogClient.getCourse(courseId);

        List<EnrollmentStatus> activeStatuses = List.of(EnrollmentStatus.PENDING, EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);
        List<Enrollment> existing = enrollmentRepository.findByEmployeeIdAndCourseIdAndStatusInWithLock(
                employeeId, courseId, activeStatuses);

        if (!existing.isEmpty()) {
            throw new BusinessException("Employee is already enrolled in this course");
        }

        if (course.getEnrolledCount() >= course.getCapacity()) {
            throw new BusinessException("No available seats for course: " + courseId);
        }

        Enrollment enrollment = Enrollment.builder()
                .employeeId(employeeId)
                .courseId(courseId)
                .status(EnrollmentStatus.PENDING)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(saved.getId())
                .employeeId(employeeId)
                .courseId(courseId)
                .build();

        persistOutboxEvent("Enrollment", String.valueOf(saved.getId()), "EnrollmentCreatedEvent",
                event, RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED);
        log.info("Persisted EnrollmentCreatedEvent to outbox for enrollment: {}", saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public EnrollmentResponse approve(Long enrollmentId) {
        log.info("Approving enrollment: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessException(
                "Cannot approve enrollment in status: " + enrollment.getStatus() + ". Expected: PENDING"
            );
        }

        Long courseId = enrollment.getCourseId();

        enrollment.setStatus(EnrollmentStatus.APPROVED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        try {
            trainingCatalogClient.incrementEnrolledCount(courseId);
        } catch (Exception e) {
            try {
                trainingCatalogClient.decrementEnrolledCount(courseId);
            } catch (Exception ex) {
                log.error("Failed to compensate enrolledCount for course {} after increment failure", courseId, ex);
            }
            throw e;
        }

        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(saved.getId())
                .employeeId(saved.getEmployeeId())
                .courseId(saved.getCourseId())
                .build();

        persistOutboxEvent("Enrollment", String.valueOf(saved.getId()), "EnrollmentApprovedEvent",
                event, RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED);
        log.info("Persisted EnrollmentApprovedEvent to outbox for enrollment: {}", saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public EnrollmentResponse complete(Long enrollmentId) {
        log.info("Completing enrollment: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.APPROVED) {
            throw new BusinessException(
                "Cannot complete enrollment in status: " + enrollment.getStatus() + ". Expected: APPROVED"
            );
        }

        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(LocalDateTime.now());
        Enrollment saved = enrollmentRepository.save(enrollment);

        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(saved.getId())
                .employeeId(saved.getEmployeeId())
                .courseId(saved.getCourseId())
                .build();

        persistOutboxEvent("Enrollment", String.valueOf(saved.getId()), "CourseCompletedEvent",
                event, RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED);
        log.info("Persisted CourseCompletedEvent to outbox for enrollment: {}", saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public EnrollmentResponse reject(Long enrollmentId) {
        log.info("Rejecting enrollment: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new BusinessException(
                "Cannot reject enrollment in status: " + enrollment.getStatus() + ". Expected: PENDING"
            );
        }

        enrollment.setStatus(EnrollmentStatus.REJECTED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        EnrollmentRejectedEvent event = EnrollmentRejectedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(saved.getId())
                .employeeId(saved.getEmployeeId())
                .courseId(saved.getCourseId())
                .build();

        persistOutboxEvent("Enrollment", String.valueOf(saved.getId()), "EnrollmentRejectedEvent",
                event, RabbitMQConstants.ROUTING_KEY_ENROLLMENT_REJECTED);
        log.info("Persisted EnrollmentRejectedEvent to outbox for enrollment: {}", saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public EnrollmentResponse cancel(Long enrollmentId) {
        log.info("Cancelling enrollment: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING && enrollment.getStatus() != EnrollmentStatus.APPROVED) {
            throw new BusinessException(
                "Cannot cancel enrollment in status: " + enrollment.getStatus() + ". Expected: PENDING or APPROVED"
            );
        }

        if (enrollment.getStatus() == EnrollmentStatus.APPROVED) {
            try {
                trainingCatalogClient.decrementEnrolledCount(enrollment.getCourseId());
            } catch (Exception e) {
                log.error("Failed to decrement enrolledCount for course {} during cancel of enrollment {}",
                        enrollment.getCourseId(), enrollmentId, e);
            }
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(saved.getId())
                .employeeId(saved.getEmployeeId())
                .courseId(saved.getCourseId())
                .build();

        persistOutboxEvent("Enrollment", String.valueOf(saved.getId()), "EnrollmentCancelledEvent",
                event, RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CANCELLED);
        log.info("Persisted EnrollmentCancelledEvent to outbox for enrollment: {}", saved.getId());

        return toResponse(saved);
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
        } catch (JacksonException e) {
            log.error("Failed to serialize {} for outbox: {}", eventType, e.getMessage());
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }

}

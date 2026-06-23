package com.poc.training.enrollment.service;

import com.poc.training.shared.dto.CourseDTO;
import com.poc.training.enrollment.client.TrainingCatalogClient;
import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import com.poc.training.shared.event.EnrollmentCreatedEvent;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TrainingCatalogClient trainingCatalogClient;
    private final RabbitTemplate rabbitTemplate;

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

        CourseDTO course = trainingCatalogClient.getCourse(courseId);

        List<EnrollmentStatus> activeStatuses = List.of(EnrollmentStatus.PENDING, EnrollmentStatus.APPROVED);
        boolean alreadyEnrolled = enrollmentRepository.existsByEmployeeIdAndCourseIdAndStatusIn(
                employeeId, courseId, activeStatuses);

        if (alreadyEnrolled) {
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

        sendAfterCommit(() -> rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED,
                event));
        log.info("Published EnrollmentCreatedEvent for enrollment: {}", saved.getId());

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

        enrollment.setStatus(EnrollmentStatus.APPROVED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        Long courseId = saved.getCourseId();
        sendAfterCommit(() -> {
            try {
                trainingCatalogClient.incrementEnrolledCount(courseId);
            } catch (Exception e) {
                log.error("Failed to increment enrolled count for course {}: {}", courseId, e.getMessage());
            }
            EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .source("enrollment-service")
                    .enrollmentId(saved.getId())
                    .employeeId(saved.getEmployeeId())
                    .courseId(saved.getCourseId())
                    .build();
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE,
                    RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED,
                    event);
        });
        log.info("Published EnrollmentApprovedEvent for enrollment: {}", saved.getId());

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

        sendAfterCommit(() -> rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED,
                event));
        log.info("Published CourseCompletedEvent for enrollment: {}", saved.getId());

        return toResponse(saved);
    }

    private void sendAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.error("Failed to execute after commit action: {}", e.getMessage());
                    }
                }
            });
        } else {
            action.run();
        }
    }
}

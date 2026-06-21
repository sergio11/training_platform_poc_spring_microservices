package com.poc.training.enrollment.service;

import com.poc.training.enrollment.client.CourseDTO;
import com.poc.training.enrollment.client.TrainingCatalogClient;
import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import com.poc.training.shared.event.EnrollmentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TrainingCatalogClient trainingCatalogClient;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "training.exchange";

    public List<Enrollment> findByEmployeeId(Long employeeId) {
        return enrollmentRepository.findByEmployeeId(employeeId);
    }

    public Enrollment findById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found with id: " + id));
    }

    public Enrollment enroll(Long employeeId, Long courseId) {
        log.info("Processing enrollment for employee {} in course {}", employeeId, courseId);

        CourseDTO course = trainingCatalogClient.getCourse(courseId);

        List<EnrollmentStatus> activeStatuses = List.of(EnrollmentStatus.PENDING, EnrollmentStatus.APPROVED);
        boolean alreadyEnrolled = enrollmentRepository.existsByEmployeeIdAndCourseIdAndStatusIn(
                employeeId, courseId, activeStatuses);

        if (alreadyEnrolled) {
            throw new RuntimeException("Employee is already enrolled in this course");
        }

        if (course.getEnrolledCount() >= course.getCapacity()) {
            throw new RuntimeException("No available seats for course: " + courseId);
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

        rabbitTemplate.convertAndSend(EXCHANGE, "enrollment.created", event);
        log.info("Published EnrollmentCreatedEvent for enrollment: {}", saved.getId());

        return saved;
    }

    public Enrollment approve(Long enrollmentId) {
        log.info("Approving enrollment: {}", enrollmentId);

        Enrollment enrollment = findById(enrollmentId);

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new RuntimeException(
                "Cannot approve enrollment in status: " + enrollment.getStatus() + ". Expected: PENDING"
            );
        }

        enrollment.setStatus(EnrollmentStatus.APPROVED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        trainingCatalogClient.incrementEnrolledCount(saved.getCourseId());

        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(saved.getId())
                .employeeId(saved.getEmployeeId())
                .courseId(saved.getCourseId())
                .build();

        rabbitTemplate.convertAndSend(EXCHANGE, "enrollment.approved", event);
        log.info("Published EnrollmentApprovedEvent for enrollment: {}", saved.getId());

        return saved;
    }

    public Enrollment complete(Long enrollmentId) {
        log.info("Completing enrollment: {}", enrollmentId);

        Enrollment enrollment = findById(enrollmentId);

        if (enrollment.getStatus() != EnrollmentStatus.APPROVED) {
            throw new RuntimeException(
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

        rabbitTemplate.convertAndSend(EXCHANGE, "course.completed", event);
        log.info("Published CourseCompletedEvent for enrollment: {}", saved.getId());

        return saved;
    }
}

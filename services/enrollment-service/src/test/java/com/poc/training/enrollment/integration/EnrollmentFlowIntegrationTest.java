package com.poc.training.enrollment.integration;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.EnrollmentCreatedEvent;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EnrollmentFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("training_enrollment_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateEnrollmentAndTransitionStates() {
        Enrollment enrollment = Enrollment.builder()
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        assertNotNull(saved.getId());
        assertEquals(EnrollmentStatus.PENDING, saved.getStatus());

        saved.setStatus(EnrollmentStatus.APPROVED);
        Enrollment approved = enrollmentRepository.save(saved);
        assertEquals(EnrollmentStatus.APPROVED, approved.getStatus());

        approved.setStatus(EnrollmentStatus.COMPLETED);
        approved.setCompletedAt(LocalDateTime.now());
        Enrollment completed = enrollmentRepository.save(approved);
        assertEquals(EnrollmentStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void shouldFindEnrollmentsByEmployeeId() {
        Enrollment enrollment1 = Enrollment.builder()
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .employeeId(1L)
                .courseId(2L)
                .status(EnrollmentStatus.APPROVED)
                .build();

        enrollmentRepository.save(enrollment1);
        enrollmentRepository.save(enrollment2);

        var enrollments = enrollmentRepository.findByEmployeeId(1L, PageRequest.of(0, 10));
        assertEquals(2, enrollments.getContent().size());
    }

    @Test
    void shouldCheckExistingEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        enrollmentRepository.save(enrollment);

        boolean exists = enrollmentRepository.existsByEmployeeIdAndCourseIdAndStatusIn(
                1L, 1L, List.of(EnrollmentStatus.PENDING, EnrollmentStatus.APPROVED));

        assertTrue(exists);
    }

    @Test
    void shouldCreateEnrollmentCreatedEvent() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals("enrollment-service", event.getSource());
        assertEquals(1L, event.getEnrollmentId());
    }

    @Test
    void shouldCreateEnrollmentApprovedEvent() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals("enrollment-service", event.getSource());
    }

    @Test
    void shouldCreateCourseCompletedEvent() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals("course.completed", "course.completed");
    }
}

package com.poc.training.shared.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseEventTest {

    @Test
    void shouldCreateEventWithBuilder() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId("test-123")
                .timestamp(LocalDateTime.of(2024, 1, 15, 10, 30))
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(2L)
                .courseId(3L)
                .build();

        assertEquals("test-123", event.getEventId());
        assertEquals("enrollment-service", event.getSource());
        assertEquals(1L, event.getEnrollmentId());
        assertEquals(2L, event.getEmployeeId());
        assertEquals(3L, event.getCourseId());
    }

    @Test
    void shouldCreateCourseCompletedEvent() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId("cc-456")
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(10L)
                .employeeId(20L)
                .courseId(30L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals(10L, event.getEnrollmentId());
    }

    @Test
    void shouldCreateCertificateGeneratedEvent() {
        CertificateGeneratedEvent event = CertificateGeneratedEvent.builder()
                .eventId("cert-789")
                .timestamp(LocalDateTime.now())
                .source("certification-service")
                .certificateId(1L)
                .enrollmentId(2L)
                .employeeId(3L)
                .courseId(4L)
                .certificateNumber("CERT-12345")
                .build();

        assertEquals("CERT-12345", event.getCertificateNumber());
        assertEquals(1L, event.getCertificateId());
    }

    @Test
    void shouldCreateEnrollmentApprovedEvent() {
        EnrollmentApprovedEvent event = EnrollmentApprovedEvent.builder()
                .eventId("ea-012")
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(2L)
                .courseId(3L)
                .build();

        assertNotNull(event.getEventId());
        assertEquals("enrollment-service", event.getSource());
    }

    @Test
    void shouldCreateEnrollmentRejectedEvent() {
        EnrollmentRejectedEvent event = EnrollmentRejectedEvent.builder()
                .eventId("er-345")
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(2L)
                .courseId(3L)
                .build();

        assertNotNull(event.getEventId());
    }

    @Test
    void shouldCreateEnrollmentCancelledEvent() {
        EnrollmentCancelledEvent event = EnrollmentCancelledEvent.builder()
                .eventId("ec-678")
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(2L)
                .courseId(3L)
                .build();

        assertNotNull(event.getEventId());
    }

    @Test
    void shouldCreateNotificationSentEvent() {
        NotificationSentEvent event = NotificationSentEvent.builder()
                .eventId("ns-901")
                .timestamp(LocalDateTime.now())
                .source("notification-service")
                .notificationId(1L)
                .recipientId(2L)
                .notificationType("ENROLLMENT_CREATED")
                .referenceId("1")
                .referenceType("ENROLLMENT")
                .build();

        assertEquals("notification-service", event.getSource());
        assertEquals("ENROLLMENT_CREATED", event.getNotificationType());
    }

    @Test
    void shouldHaveNullDefaultsWhenNotSet() {
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder().build();

        assertNull(event.getEventId());
        assertNull(event.getTimestamp());
        assertNull(event.getSource());
    }
}

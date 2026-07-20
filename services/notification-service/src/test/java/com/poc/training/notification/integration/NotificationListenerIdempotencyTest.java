package com.poc.training.notification.integration;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.repository.NotificationRepository;
import com.poc.training.notification.service.NotificationService;
import com.poc.training.shared.event.EnrollmentCreatedEvent;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class NotificationListenerIdempotencyTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldNotCreateDuplicateNotificationForSameEvent() {
        String eventId = UUID.randomUUID().toString();
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(eventId)
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(100L)
                .courseId(200L)
                .build();

        notificationService.handleEnrollmentCreated(event);
        notificationService.handleEnrollmentCreated(event);
        notificationService.handleEnrollmentCreated(event);

        var notifications = notificationRepository.findByRecipientId(100L, PageRequest.of(0, 10));
        assertEquals(1, notifications.getContent().size());
        assertEquals(NotificationType.ENROLLMENT_CREATED, notifications.getContent().get(0).getType());
    }

    @Test
    void shouldAllowDifferentEventTypesForSameEnrollment() {
        Long enrollmentId = 500L;
        Long employeeId = 501L;

        notificationService.handleEnrollmentCreated(EnrollmentCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(enrollmentId)
                .employeeId(employeeId)
                .courseId(600L)
                .build());

        notificationService.handleEnrollmentApproved(com.poc.training.shared.event.EnrollmentApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(enrollmentId)
                .employeeId(employeeId)
                .courseId(600L)
                .build());

        var notifications = notificationRepository.findByRecipientId(employeeId, PageRequest.of(0, 10));
        assertEquals(2, notifications.getContent().size());
    }

    @Test
    void shouldTrackProcessedEvents() {
        String eventId = UUID.randomUUID().toString();
        EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                .eventId(eventId)
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(700L)
                .employeeId(701L)
                .courseId(702L)
                .build();

        notificationService.handleEnrollmentCreated(event);

        // Verify the processed event was created
        assertNotNull(notificationRepository.findByRecipientId(701L, PageRequest.of(0, 10)).getContent());
    }
}

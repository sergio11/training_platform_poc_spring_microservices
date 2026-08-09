package com.poc.training.enrollment.integration;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.shared.model.OutboxEvent;
import com.poc.training.shared.repository.OutboxEventRepository;
import com.poc.training.shared.scheduler.OutboxEventScheduler;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class OutboxEventSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventScheduler outboxEventScheduler;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        enrollmentRepository.deleteAll();
    }

    @Test
    void shouldProcessPendingOutboxEvents() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Enrollment")
                .aggregateId("1")
                .eventType("EnrollmentCreatedEvent")
                .payload("{\"eventId\":\"test-123\"}")
                .routingKey(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED)
                .processed(false)
                .build();
        outboxEventRepository.save(event);

        outboxEventScheduler.processPendingEvents();

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertTrue(updated.isProcessed());
    }

    @Test
    void shouldNotReprocessAlreadyProcessedEvents() {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("Enrollment")
                .aggregateId("2")
                .eventType("EnrollmentApprovedEvent")
                .payload("{\"eventId\":\"test-456\"}")
                .routingKey(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED)
                .processed(true)
                .build();
        outboxEventRepository.save(event);

        outboxEventScheduler.processPendingEvents();

        // Should still be processed (no duplicate processing)
        OutboxEvent found = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertTrue(found.isProcessed());
    }

    @Test
    void shouldProcessMultiplePendingEvents() {
        for (int i = 0; i < 5; i++) {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("Enrollment")
                    .aggregateId(String.valueOf(i))
                    .eventType("EnrollmentCreatedEvent")
                    .payload("{\"eventId\":\"multi-" + i + "\"}")
                    .routingKey(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED)
                    .processed(false)
                    .build());
        }

        outboxEventScheduler.processPendingEvents();

        long processedCount = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)).stream().count();
        assertEquals(0, processedCount);
    }
}

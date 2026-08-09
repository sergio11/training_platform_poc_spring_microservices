package com.poc.training.shared.scheduler;

import com.poc.training.shared.model.OutboxEvent;
import com.poc.training.shared.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private OutboxEventScheduler outboxEventScheduler;

    @SuppressWarnings("unchecked")
    private void stubTransactionTemplate() {
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any(Consumer.class));
    }

    @Test
    void shouldProcessPendingEvents() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Enrollment")
                .aggregateId("agg-1")
                .eventType("EnrollmentCreatedEvent")
                .payload("{\"id\":1}")
                .routingKey("enrollment.created")
                .createdAt(LocalDateTime.now())
                .processed(false)
                .build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate).send(eq("training.exchange"), eq("enrollment.created"), any());
        verify(outboxEventRepository).save(event);
    }

    @Test
    void shouldDoNothingWhenNoPendingEvents() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(Collections.emptyList());

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void shouldHandleMultipleEvents() {
        OutboxEvent event1 = OutboxEvent.builder()
                .id(1L).aggregateType("Enrollment").aggregateId("agg-1")
                .eventType("EnrollmentCreatedEvent").payload("{\"id\":1}")
                .routingKey("enrollment.created").createdAt(LocalDateTime.now()).processed(false).build();
        OutboxEvent event2 = OutboxEvent.builder()
                .id(2L).aggregateType("Enrollment").aggregateId("agg-2")
                .eventType("EnrollmentApprovedEvent").payload("{\"id\":2}")
                .routingKey("enrollment.approved").createdAt(LocalDateTime.now()).processed(false).build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(List.of(event1, event2));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate).send(eq("training.exchange"), eq("enrollment.created"), any());
        verify(rabbitTemplate).send(eq("training.exchange"), eq("enrollment.approved"), any());
        verify(outboxEventRepository).save(event1);
        verify(outboxEventRepository).save(event2);
    }

    @Test
    void shouldHandleEventSendFailure() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L).aggregateType("Enrollment").aggregateId("agg-1")
                .eventType("EnrollmentCreatedEvent").payload("{\"id\":1}")
                .routingKey("enrollment.created").createdAt(LocalDateTime.now()).processed(false).build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(List.of(event));
        stubTransactionTemplate();
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate).send(anyString(), anyString(), any());

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository, never()).save(event);
    }

    @Test
    void shouldResolveAllEventTypes() {
        Map<String, String> expectedTypes = Map.of(
                "EnrollmentCreatedEvent", "com.poc.training.shared.event.EnrollmentCreatedEvent",
                "EnrollmentApprovedEvent", "com.poc.training.shared.event.EnrollmentApprovedEvent",
                "EnrollmentRejectedEvent", "com.poc.training.shared.event.EnrollmentRejectedEvent",
                "EnrollmentCancelledEvent", "com.poc.training.shared.event.EnrollmentCancelledEvent",
                "CourseCompletedEvent", "com.poc.training.shared.event.CourseCompletedEvent",
                "CertificateGeneratedEvent", "com.poc.training.shared.event.CertificateGeneratedEvent",
                "EmployeeCreatedEvent", "com.poc.training.shared.event.EmployeeCreatedEvent",
                "EmployeeUpdatedEvent", "com.poc.training.shared.event.EmployeeUpdatedEvent",
                "EmployeeDeactivatedEvent", "com.poc.training.shared.event.EmployeeDeactivatedEvent",
                "NotificationSentEvent", "com.poc.training.shared.event.NotificationSentEvent"
        );

        int[] counter = {1};
        List<OutboxEvent> events = expectedTypes.entrySet().stream()
                .map(e -> OutboxEvent.builder()
                        .id((long) counter[0]++).aggregateType("Test").aggregateId("agg-" + counter[0])
                        .eventType(e.getKey()).payload("{}").routingKey("test.key")
                        .createdAt(LocalDateTime.now()).processed(false).build())
                .toList();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(events);
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository, times(10)).save(any());
    }

    @Test
    void shouldThrowForUnknownEventType() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L).aggregateType("Test").aggregateId("agg-1")
                .eventType("UnknownEvent").payload("{}").routingKey("test.key")
                .createdAt(LocalDateTime.now()).processed(false).build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100)))
                .thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository, never()).save(any());
    }
}

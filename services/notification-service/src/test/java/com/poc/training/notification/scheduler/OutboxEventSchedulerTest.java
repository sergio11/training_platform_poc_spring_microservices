package com.poc.training.notification.scheduler;

import com.poc.training.notification.model.OutboxEvent;
import com.poc.training.notification.repository.OutboxEventRepository;
import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
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

    private OutboxEvent buildEvent(String eventType, Long id) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setAggregateType("Notification");
        event.setAggregateId("1");
        event.setEventType(eventType);
        event.setPayload("{\"test\":\"payload\"}");
        event.setRoutingKey("notification.sent");
        event.setProcessed(false);
        return event;
    }

    @SuppressWarnings("unchecked")
    private void stubTransactionTemplate() {
        doAnswer(inv -> {
            Consumer<Object> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any(Consumer.class));
    }

    @Test
    void shouldProcessPendingEventsSuccessfully() {
        OutboxEvent event = buildEvent("NotificationSentEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).save(event);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE),
                eq("notification.sent"),
                eq("{\"test\":\"payload\"}"));
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldHandleSendFailure() {
        OutboxEvent event = buildEvent("NotificationSentEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        stubTransactionTemplate();
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> outboxEventScheduler.processPendingEvents());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        assertFalse(event.isProcessed());
    }

    @Test
    void shouldHandleEmptyPendingList() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void shouldProcessMultipleEvents() {
        OutboxEvent e1 = buildEvent("NotificationSentEvent", 1L);
        OutboxEvent e2 = buildEvent("NotificationSentEvent", 2L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e1, e2));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), anyString());
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
        assertTrue(e1.isProcessed());
        assertTrue(e2.isProcessed());
    }
}

package com.poc.training.notification.scheduler;

import com.poc.training.shared.model.OutboxEvent;
import com.poc.training.shared.repository.OutboxEventRepository;
import com.poc.training.shared.scheduler.OutboxEventScheduler;
import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
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
        event.setRetryCount(0);
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
        when(outboxEventRepository.findPendingEventsWithLock(any(Pageable.class))).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).save(event);
        verify(rabbitTemplate).send(
                eq(RabbitMQConstants.EXCHANGE),
                eq("notification.sent"),
                any(Message.class));
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldRetryOnSendFailure() {
        OutboxEvent event = buildEvent("NotificationSentEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(any(Pageable.class))).thenReturn(List.of(event));
        stubTransactionTemplate();
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertDoesNotThrow(() -> outboxEventScheduler.processPendingEvents());
        verify(outboxEventRepository).save(event);
        assertFalse(event.isProcessed());
        assertEquals(1, event.getRetryCount());
    }

    @Test
    void shouldHandleEmptyPendingList() {
        when(outboxEventRepository.findPendingEventsWithLock(any(Pageable.class))).thenReturn(List.of());
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void shouldProcessMultipleEvents() {
        OutboxEvent e1 = buildEvent("NotificationSentEvent", 1L);
        OutboxEvent e2 = buildEvent("NotificationSentEvent", 2L);
        when(outboxEventRepository.findPendingEventsWithLock(any(Pageable.class))).thenReturn(List.of(e1, e2));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, times(2)).send(anyString(), anyString(), any(Message.class));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
        assertTrue(e1.isProcessed());
        assertTrue(e2.isProcessed());
    }
}

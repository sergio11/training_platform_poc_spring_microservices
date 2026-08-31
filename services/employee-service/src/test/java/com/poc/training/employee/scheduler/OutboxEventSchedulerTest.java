package com.poc.training.employee.scheduler;

import com.poc.training.shared.model.OutboxEvent;
import com.poc.training.shared.repository.OutboxEventRepository;
import com.poc.training.shared.scheduler.OutboxEventScheduler;
import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
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
        event.setAggregateType("Employee");
        event.setAggregateId("1");
        event.setEventType(eventType);
        event.setPayload("{\"test\":\"payload\"}");
        event.setRoutingKey("employee.created");
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
    void shouldProcessPendingEvents() {
        OutboxEvent event = buildEvent("EmployeeCreatedEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100))).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).save(event);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("employee.created"), any(Message.class));
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldProcessMultipleEvents() {
        OutboxEvent event1 = buildEvent("EmployeeCreatedEvent", 1L);
        OutboxEvent event2 = buildEvent("EmployeeDeactivatedEvent", 2L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100))).thenReturn(List.of(event1, event2));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, times(2)).send(eq(RabbitMQConstants.EXCHANGE), eq("employee.created"), any(Message.class));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void shouldSkipWhenNoPendingEvents() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100))).thenReturn(List.of());

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void shouldRetryWhenSendFails() {
        OutboxEvent event = buildEvent("EmployeeCreatedEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100))).thenReturn(List.of(event));
        stubTransactionTemplate();
        doThrow(new RuntimeException("Connection refused")).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertDoesNotThrow(() -> outboxEventScheduler.processPendingEvents());
        assertFalse(event.isProcessed());
        assertEquals(1, event.getRetryCount());
        verify(outboxEventRepository).save(event);
    }

    @Test
    void shouldVerifyTypeIdHeader() {
        OutboxEvent event = buildEvent("EmployeeCreatedEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100))).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("employee.created"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.EmployeeCreatedEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
    }
}

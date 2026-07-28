package com.poc.training.employee.scheduler;

import com.poc.training.employee.model.OutboxEvent;
import com.poc.training.employee.repository.OutboxEventRepository;
import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    void shouldProcessPendingEvents() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Employee")
                .aggregateId("100")
                .eventType("EmployeeCreatedEvent")
                .payload("{\"key\":\"value\"}")
                .routingKey(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED)
                .processed(false)
                .build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).findByProcessedFalseOrderByCreatedAtAsc();
        verify(transactionTemplate).executeWithoutResult(any());
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConstants.EXCHANGE,
                RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED,
                "{\"key\":\"value\"}");
        verify(outboxEventRepository).save(event);
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldSkipWhenNoPendingEvents() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(Collections.emptyList());

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).findByProcessedFalseOrderByCreatedAtAsc();
        verify(transactionTemplate, never()).executeWithoutResult(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    void shouldProcessMultipleEvents() {
        OutboxEvent event1 = OutboxEvent.builder()
                .id(1L).aggregateType("Employee").aggregateId("1")
                .eventType("EmployeeCreatedEvent").payload("p1")
                .routingKey(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED).processed(false).build();
        OutboxEvent event2 = OutboxEvent.builder()
                .id(2L).aggregateType("Employee").aggregateId("2")
                .eventType("EmployeeDeactivatedEvent").payload("p2")
                .routingKey(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_DEACTIVATED).processed(false).build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event1, event2));
        doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate).convertAndSend(RabbitMQConstants.EXCHANGE, RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED, "p1");
        verify(rabbitTemplate).convertAndSend(RabbitMQConstants.EXCHANGE, RabbitMQConstants.ROUTING_KEY_EMPLOYEE_DEACTIVATED, "p2");
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowWhenSendFails() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L).aggregateType("Employee").aggregateId("1")
                .eventType("EmployeeCreatedEvent").payload("p")
                .routingKey(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED).processed(false).build();

        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> outboxEventScheduler.processPendingEvents());
    }
}

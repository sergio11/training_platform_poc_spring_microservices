package com.poc.training.certification.scheduler;

import com.poc.training.certification.model.OutboxEvent;
import com.poc.training.certification.repository.OutboxEventRepository;
import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
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
        event.setAggregateType("Certificate");
        event.setAggregateId("1");
        event.setEventType(eventType);
        event.setPayload("{\"test\":\"payload\"}");
        event.setRoutingKey("certificate.generated");
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
    void shouldProcessCertificateGeneratedEvent() {
        OutboxEvent event = buildEvent("CertificateGeneratedEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).save(event);
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("certificate.generated"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.CertificateGeneratedEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldThrowForUnknownEventType() {
        OutboxEvent event = buildEvent("UnknownEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        stubTransactionTemplate();

        assertThrows(IllegalArgumentException.class, () -> outboxEventScheduler.processPendingEvents());
    }

    @Test
    void shouldRethrowWhenSendFails() {
        OutboxEvent event = buildEvent("CertificateGeneratedEvent", 1L);
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        stubTransactionTemplate();
        doThrow(new RuntimeException("Connection refused")).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertThrows(RuntimeException.class, () -> outboxEventScheduler.processPendingEvents());
        verify(outboxEventRepository).save(event);
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldHandleEmptyPendingEvents() {
        when(outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}

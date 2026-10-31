package com.poc.training.enrollment.scheduler;

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
        event.setAggregateType("Enrollment");
        event.setAggregateId("1");
        event.setEventType(eventType);
        event.setPayload("{\"test\":\"payload\"}");
        event.setRoutingKey("enrollment.created");
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
    void shouldProcessCreatedEvent() {
        OutboxEvent event = buildEvent("EnrollmentCreatedEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(outboxEventRepository).save(event);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("enrollment.created"), any(Message.class));
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldProcessApprovedEvent() {
        OutboxEvent event = buildEvent("EnrollmentApprovedEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("enrollment.created"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.EnrollmentApprovedEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
    }

    @Test
    void shouldProcessRejectedEvent() {
        OutboxEvent event = buildEvent("EnrollmentRejectedEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("enrollment.created"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.EnrollmentRejectedEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
    }

    @Test
    void shouldProcessCancelledEvent() {
        OutboxEvent event = buildEvent("EnrollmentCancelledEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("enrollment.created"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.EnrollmentCancelledEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
    }

    @Test
    void shouldProcessCourseCompletedEvent() {
        OutboxEvent event = buildEvent("CourseCompletedEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("enrollment.created"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.CourseCompletedEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
    }

    @Test
    void shouldProcessCertificateGeneratedEvent() {
        OutboxEvent event = buildEvent("CertificateGeneratedEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMQConstants.EXCHANGE), eq("enrollment.created"), msgCaptor.capture());
        assertEquals("com.poc.training.shared.event.CertificateGeneratedEvent",
                msgCaptor.getValue().getMessageProperties().getHeader("__TypeId__"));
    }

    @Test
    void shouldRetryForUnknownEventType() {
        OutboxEvent event = buildEvent("UnknownEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();

        assertDoesNotThrow(() -> outboxEventScheduler.processPendingEvents());
        verify(outboxEventRepository).save(event);
        assertEquals(1, event.getRetryCount());
    }

    @Test
    void shouldRetryWhenSendFails() {
        OutboxEvent event = buildEvent("EnrollmentCreatedEvent", 1L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(event));
        stubTransactionTemplate();
        doThrow(new RuntimeException("Connection refused")).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertDoesNotThrow(() -> outboxEventScheduler.processPendingEvents());
        verify(outboxEventRepository).save(event);
        assertFalse(event.isProcessed());
        assertEquals(1, event.getRetryCount());
    }

    @Test
    void shouldHandleMultipleEvents() {
        OutboxEvent e1 = buildEvent("EnrollmentCreatedEvent", 1L);
        OutboxEvent e2 = buildEvent("EnrollmentApprovedEvent", 2L);
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of(e1, e2));
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, times(2)).send(anyString(), anyString(), any(Message.class));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void shouldHandleEmptyPendingEvents() {
        when(outboxEventRepository.findPendingEventsWithLock(PageRequest.of(0, 100), OutboxEventScheduler.MAX_RETRIES)).thenReturn(List.of());
        stubTransactionTemplate();

        outboxEventScheduler.processPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}

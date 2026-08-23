package com.poc.training.catalog.config;

import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void shouldCreateTrainingExchange() {
        TopicExchange exchange = config.trainingExchange();
        assertNotNull(exchange);
        assertEquals(RabbitMQConstants.EXCHANGE, exchange.getName());
    }

    @Test
    void shouldCreateEnrollmentApprovedQueue() {
        Queue queue = config.enrollmentApprovedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConstants.QUEUE_ENROLLMENT_APPROVED, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentCancelledQueue() {
        Queue queue = config.enrollmentCancelledQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConstants.QUEUE_ENROLLMENT_CANCELLED, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentRejectedQueue() {
        Queue queue = config.enrollmentRejectedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConstants.QUEUE_ENROLLMENT_REJECTED, queue.getName());
    }

    @Test
    void shouldCreateDeadLetterQueue() {
        Queue queue = config.deadLetterQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.DLQ, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentApprovedBinding() {
        Queue queue = config.enrollmentApprovedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentApprovedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEnrollmentCancelledBinding() {
        Queue queue = config.enrollmentCancelledQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentCancelledBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEnrollmentRejectedBinding() {
        Queue queue = config.enrollmentRejectedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentRejectedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateDeadLetterBinding() {
        Queue queue = config.deadLetterQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.deadLetterBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateJsonMessageConverter() {
        assertNotNull(config.jsonMessageConverter(new ObjectMapper()));
    }
}

package com.poc.training.certification.config;

import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

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
    void shouldCreateCourseCompletedQueue() {
        Queue queue = config.courseCompletedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_COURSE_COMPLETED, queue.getName());
    }

    @Test
    void shouldCreateDeadLetterQueue() {
        Queue queue = config.deadLetterQueue();
        assertNotNull(queue);
        assertEquals("certification.dlq", queue.getName());
    }

    @Test
    void shouldCreateCourseCompletedBinding() {
        Queue queue = config.courseCompletedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.courseCompletedBinding(queue, exchange);
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
        assertNotNull(config.jsonMessageConverter());
    }
}

package com.poc.training.enrollment.config;

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
    void shouldCreateJsonMessageConverter() {
        assertNotNull(config.jsonMessageConverter(new ObjectMapper()));
    }
}

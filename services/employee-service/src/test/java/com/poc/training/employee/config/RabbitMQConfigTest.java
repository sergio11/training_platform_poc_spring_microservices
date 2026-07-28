package com.poc.training.employee.config;

import com.poc.training.shared.event.RabbitMQConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void shouldCreateTopicExchange() {
        TopicExchange exchange = config.trainingExchange();

        assertNotNull(exchange);
        assertEquals(RabbitMQConstants.EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
    }

    @Test
    void shouldCreateJsonMessageConverter() {
        MessageConverter converter = config.jsonMessageConverter();

        assertNotNull(converter);
    }
}

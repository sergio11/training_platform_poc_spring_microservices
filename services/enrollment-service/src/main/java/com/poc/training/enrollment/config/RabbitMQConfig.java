package com.poc.training.enrollment.config;

import com.poc.training.shared.converter.Jackson3JsonMessageConverter;
import com.poc.training.shared.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange trainingExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConstants.EXCHANGE).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson3JsonMessageConverter(objectMapper);
    }
}

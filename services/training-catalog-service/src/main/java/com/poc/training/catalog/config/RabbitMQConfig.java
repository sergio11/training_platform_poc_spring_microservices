package com.poc.training.catalog.config;

import com.poc.training.shared.converter.Jackson3JsonMessageConverter;
import com.poc.training.shared.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DLQ = "catalog.dlq";

    @Bean
    public TopicExchange trainingExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConstants.EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue enrollmentApprovedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_APPROVED)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    public Queue enrollmentCancelledQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_CANCELLED)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding enrollmentApprovedBinding(Queue enrollmentApprovedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentApprovedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED);
    }

    @Bean
    public Binding enrollmentCancelledBinding(Queue enrollmentCancelledQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentCancelledQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CANCELLED);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(trainingExchange).with(DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson3JsonMessageConverter();
    }
}

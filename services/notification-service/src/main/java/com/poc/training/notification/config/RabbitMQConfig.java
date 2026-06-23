package com.poc.training.notification.config;

import com.poc.training.shared.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange trainingExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConstants.EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue enrollmentCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_CREATED).build();
    }

    @Bean
    public Queue enrollmentApprovedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_APPROVED).build();
    }

    @Bean
    public Queue courseCompletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_COURSE_COMPLETED).build();
    }

    @Bean
    public Queue certificateGeneratedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_CERTIFICATE_GENERATED).build();
    }

    @Bean
    public Binding enrollmentCreatedBinding(Queue enrollmentCreatedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentCreatedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED);
    }

    @Bean
    public Binding enrollmentApprovedBinding(Queue enrollmentApprovedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentApprovedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED);
    }

    @Bean
    public Binding courseCompletedBinding(Queue courseCompletedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(courseCompletedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED);
    }

    @Bean
    public Binding certificateGeneratedBinding(Queue certificateGeneratedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(certificateGeneratedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

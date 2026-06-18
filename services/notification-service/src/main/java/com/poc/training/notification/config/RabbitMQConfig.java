package com.poc.training.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "training.exchange";

    @Bean
    public TopicExchange trainingExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue enrollmentCreatedQueue() {
        return QueueBuilder.durable("enrollment.created").build();
    }

    @Bean
    public Queue enrollmentApprovedQueue() {
        return QueueBuilder.durable("enrollment.approved").build();
    }

    @Bean
    public Queue certificateGeneratedQueue() {
        return QueueBuilder.durable("certificate.generated").build();
    }

    @Bean
    public Binding enrollmentCreatedBinding(Queue enrollmentCreatedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentCreatedQueue).to(trainingExchange).with("enrollment.created");
    }

    @Bean
    public Binding enrollmentApprovedBinding(Queue enrollmentApprovedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentApprovedQueue).to(trainingExchange).with("enrollment.approved");
    }

    @Bean
    public Binding certificateGeneratedBinding(Queue certificateGeneratedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(certificateGeneratedQueue).to(trainingExchange).with("certificate.generated");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

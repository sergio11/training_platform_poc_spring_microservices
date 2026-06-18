package com.poc.training.certification.config;

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
    public Queue courseCompletedQueue() {
        return QueueBuilder.durable("course.completed").build();
    }

    @Bean
    public Binding courseCompletedBinding(Queue courseCompletedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(courseCompletedQueue).to(trainingExchange).with("course.completed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

package com.poc.training.certification.config;

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
    public Queue courseCompletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_COURSE_COMPLETED).build();
    }

    @Bean
    public Binding courseCompletedBinding(Queue courseCompletedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(courseCompletedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

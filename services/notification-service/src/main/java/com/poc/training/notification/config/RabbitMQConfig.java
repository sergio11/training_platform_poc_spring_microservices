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
    public Queue notificationSentQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_NOTIFICATION_SENT)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("notification.dlq").build();
    }

    @Bean
    public Queue enrollmentCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_CREATED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue enrollmentApprovedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_APPROVED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue courseCompletedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_COURSE_COMPLETED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue certificateGeneratedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_CERTIFICATE_GENERATED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue enrollmentRejectedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_ENROLLMENT_REJECTED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
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
    public Binding enrollmentRejectedBinding(Queue enrollmentRejectedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentRejectedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_REJECTED);
    }

    @Bean
    public Binding notificationSentBinding(Queue notificationSentQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(notificationSentQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_NOTIFICATION_SENT);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(trainingExchange).with("notification.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

package com.poc.training.notification.config;

import com.poc.training.shared.converter.Jackson3JsonMessageConverter;
import com.poc.training.shared.event.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NOTIFICATION_SENT = "notification.notification.sent";
    public static final String QUEUE_ENROLLMENT_CREATED = "notification.enrollment.created";
    public static final String QUEUE_ENROLLMENT_APPROVED = "notification.enrollment.approved";
    public static final String QUEUE_CERTIFICATE_GENERATED = "notification.certificate.generated";
    public static final String QUEUE_ENROLLMENT_REJECTED = "notification.enrollment.rejected";
    public static final String QUEUE_ENROLLMENT_CANCELLED = "notification.enrollment.cancelled";
    public static final String QUEUE_COURSE_COMPLETED = "notification.course.completed";
    public static final String QUEUE_EMPLOYEE_UPDATED = "notification.employee.updated";
    public static final String QUEUE_EMPLOYEE_CREATED = "notification.employee.created";
    public static final String QUEUE_EMPLOYEE_DEACTIVATED = "notification.employee.deactivated";

    @Bean
    public TopicExchange trainingExchange() {
        return ExchangeBuilder.topicExchange(RabbitMQConstants.EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationSentQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_SENT)
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
        return QueueBuilder.durable(QUEUE_ENROLLMENT_CREATED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue enrollmentApprovedQueue() {
        return QueueBuilder.durable(QUEUE_ENROLLMENT_APPROVED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue certificateGeneratedQueue() {
        return QueueBuilder.durable(QUEUE_CERTIFICATE_GENERATED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue enrollmentRejectedQueue() {
        return QueueBuilder.durable(QUEUE_ENROLLMENT_REJECTED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue enrollmentCancelledQueue() {
        return QueueBuilder.durable(QUEUE_ENROLLMENT_CANCELLED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue courseCompletedQueue() {
        return QueueBuilder.durable(QUEUE_COURSE_COMPLETED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue employeeUpdatedQueue() {
        return QueueBuilder.durable(QUEUE_EMPLOYEE_UPDATED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue employeeCreatedQueue() {
        return QueueBuilder.durable(QUEUE_EMPLOYEE_CREATED)
                .deadLetterExchange("")
                .deadLetterRoutingKey("notification.dlq")
                .build();
    }

    @Bean
    public Queue employeeDeactivatedQueue() {
        return QueueBuilder.durable(QUEUE_EMPLOYEE_DEACTIVATED)
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
    public Binding certificateGeneratedBinding(Queue certificateGeneratedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(certificateGeneratedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED);
    }

    @Bean
    public Binding enrollmentRejectedBinding(Queue enrollmentRejectedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentRejectedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_REJECTED);
    }

    @Bean
    public Binding enrollmentCancelledBinding(Queue enrollmentCancelledQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(enrollmentCancelledQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CANCELLED);
    }

    @Bean
    public Binding courseCompletedBinding(Queue courseCompletedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(courseCompletedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED);
    }

    @Bean
    public Binding employeeUpdatedBinding(Queue employeeUpdatedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(employeeUpdatedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_UPDATED);
    }

    @Bean
    public Binding employeeCreatedBinding(Queue employeeCreatedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(employeeCreatedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED);
    }

    @Bean
    public Binding employeeDeactivatedBinding(Queue employeeDeactivatedQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(employeeDeactivatedQueue).to(trainingExchange).with(RabbitMQConstants.ROUTING_KEY_EMPLOYEE_DEACTIVATED);
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
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson3JsonMessageConverter(objectMapper);
    }
}

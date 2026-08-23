package com.poc.training.notification.config;

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
    void shouldCreateDeadLetterQueue() {
        Queue queue = config.deadLetterQueue();
        assertNotNull(queue);
        assertEquals("notification.dlq", queue.getName());
    }

    @Test
    void shouldCreateNotificationSentQueue() {
        Queue queue = config.notificationSentQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_NOTIFICATION_SENT, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentCreatedQueue() {
        Queue queue = config.enrollmentCreatedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_ENROLLMENT_CREATED, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentApprovedQueue() {
        Queue queue = config.enrollmentApprovedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_ENROLLMENT_APPROVED, queue.getName());
    }

    @Test
    void shouldCreateCertificateGeneratedQueue() {
        Queue queue = config.certificateGeneratedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_CERTIFICATE_GENERATED, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentRejectedQueue() {
        Queue queue = config.enrollmentRejectedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_ENROLLMENT_REJECTED, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentCancelledQueue() {
        Queue queue = config.enrollmentCancelledQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_ENROLLMENT_CANCELLED, queue.getName());
    }

    @Test
    void shouldCreateCourseCompletedQueue() {
        Queue queue = config.courseCompletedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_COURSE_COMPLETED, queue.getName());
    }

    @Test
    void shouldCreateEmployeeUpdatedQueue() {
        Queue queue = config.employeeUpdatedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_EMPLOYEE_UPDATED, queue.getName());
    }

    @Test
    void shouldCreateEmployeeCreatedQueue() {
        Queue queue = config.employeeCreatedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_EMPLOYEE_CREATED, queue.getName());
    }

    @Test
    void shouldCreateEmployeeDeactivatedQueue() {
        Queue queue = config.employeeDeactivatedQueue();
        assertNotNull(queue);
        assertEquals(RabbitMQConfig.QUEUE_EMPLOYEE_DEACTIVATED, queue.getName());
    }

    @Test
    void shouldCreateEnrollmentCreatedBinding() {
        Queue queue = config.enrollmentCreatedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentCreatedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEnrollmentApprovedBinding() {
        Queue queue = config.enrollmentApprovedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentApprovedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateCertificateGeneratedBinding() {
        Queue queue = config.certificateGeneratedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.certificateGeneratedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEnrollmentRejectedBinding() {
        Queue queue = config.enrollmentRejectedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentRejectedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEnrollmentCancelledBinding() {
        Queue queue = config.enrollmentCancelledQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.enrollmentCancelledBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateCourseCompletedBinding() {
        Queue queue = config.courseCompletedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.courseCompletedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEmployeeUpdatedBinding() {
        Queue queue = config.employeeUpdatedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.employeeUpdatedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEmployeeCreatedBinding() {
        Queue queue = config.employeeCreatedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.employeeCreatedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateEmployeeDeactivatedBinding() {
        Queue queue = config.employeeDeactivatedQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.employeeDeactivatedBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateNotificationSentBinding() {
        Queue queue = config.notificationSentQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.notificationSentBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateDeadLetterBinding() {
        Queue queue = config.deadLetterQueue();
        TopicExchange exchange = config.trainingExchange();
        Binding binding = config.deadLetterBinding(queue, exchange);
        assertNotNull(binding);
    }

    @Test
    void shouldCreateJsonMessageConverter() {
        assertNotNull(config.jsonMessageConverter(new ObjectMapper()));
    }
}

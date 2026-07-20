package com.poc.training.shared.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RabbitMQConstantsTest {

    @Test
    void shouldHaveCorrectExchangeName() {
        assertEquals("training.exchange", RabbitMQConstants.EXCHANGE);
    }

    @Test
    void shouldHaveCorrectRoutingKeys() {
        assertEquals("enrollment.created", RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED);
        assertEquals("enrollment.approved", RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED);
        assertEquals("enrollment.rejected", RabbitMQConstants.ROUTING_KEY_ENROLLMENT_REJECTED);
        assertEquals("enrollment.cancelled", RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CANCELLED);
        assertEquals("course.completed", RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED);
        assertEquals("certificate.generated", RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED);
        assertEquals("notification.sent", RabbitMQConstants.ROUTING_KEY_NOTIFICATION_SENT);
    }

    @Test
    void shouldHaveCorrectQueueNames() {
        assertEquals("enrollment.created", RabbitMQConstants.QUEUE_ENROLLMENT_CREATED);
        assertEquals("enrollment.approved", RabbitMQConstants.QUEUE_ENROLLMENT_APPROVED);
        assertEquals("enrollment.rejected", RabbitMQConstants.QUEUE_ENROLLMENT_REJECTED);
        assertEquals("enrollment.cancelled", RabbitMQConstants.QUEUE_ENROLLMENT_CANCELLED);
        assertEquals("course.completed", RabbitMQConstants.QUEUE_COURSE_COMPLETED);
        assertEquals("certificate.generated", RabbitMQConstants.QUEUE_CERTIFICATE_GENERATED);
        assertEquals("notification.sent", RabbitMQConstants.QUEUE_NOTIFICATION_SENT);
    }

    @Test
    void shouldHaveRoutingKeysMatchingQueueNames() {
        assertEquals(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CREATED, RabbitMQConstants.QUEUE_ENROLLMENT_CREATED);
        assertEquals(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_APPROVED, RabbitMQConstants.QUEUE_ENROLLMENT_APPROVED);
        assertEquals(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_REJECTED, RabbitMQConstants.QUEUE_ENROLLMENT_REJECTED);
        assertEquals(RabbitMQConstants.ROUTING_KEY_ENROLLMENT_CANCELLED, RabbitMQConstants.QUEUE_ENROLLMENT_CANCELLED);
        assertEquals(RabbitMQConstants.ROUTING_KEY_COURSE_COMPLETED, RabbitMQConstants.QUEUE_COURSE_COMPLETED);
        assertEquals(RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED, RabbitMQConstants.QUEUE_CERTIFICATE_GENERATED);
        assertEquals(RabbitMQConstants.ROUTING_KEY_NOTIFICATION_SENT, RabbitMQConstants.QUEUE_NOTIFICATION_SENT);
    }
}

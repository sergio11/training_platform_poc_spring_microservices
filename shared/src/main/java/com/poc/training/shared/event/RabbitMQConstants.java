package com.poc.training.shared.event;

public final class RabbitMQConstants {

    private RabbitMQConstants() {}

    public static final String EXCHANGE = "training.exchange";

    public static final String ROUTING_KEY_ENROLLMENT_CREATED = "enrollment.created";
    public static final String ROUTING_KEY_ENROLLMENT_APPROVED = "enrollment.approved";
    public static final String ROUTING_KEY_COURSE_COMPLETED = "course.completed";
    public static final String ROUTING_KEY_CERTIFICATE_GENERATED = "certificate.generated";
    public static final String ROUTING_KEY_NOTIFICATION_SENT = "notification.sent";

    public static final String QUEUE_ENROLLMENT_CREATED = "enrollment.created";
    public static final String QUEUE_ENROLLMENT_APPROVED = "enrollment.approved";
    public static final String QUEUE_COURSE_COMPLETED = "course.completed";
    public static final String QUEUE_CERTIFICATE_GENERATED = "certificate.generated";
    public static final String QUEUE_NOTIFICATION_SENT = "notification.sent";
}

package com.poc.training.shared.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SmartMessageConverter;

import java.util.Set;

public class Jackson3JsonMessageConverter implements SmartMessageConverter {

    private static final Logger log = LoggerFactory.getLogger(Jackson3JsonMessageConverter.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "com.poc.training.shared.event.EnrollmentCreatedEvent",
            "com.poc.training.shared.event.EnrollmentApprovedEvent",
            "com.poc.training.shared.event.EnrollmentRejectedEvent",
            "com.poc.training.shared.event.EnrollmentCancelledEvent",
            "com.poc.training.shared.event.CourseCompletedEvent",
            "com.poc.training.shared.event.CertificateGeneratedEvent",
            "com.poc.training.shared.event.NotificationSentEvent",
            "com.poc.training.shared.event.EmployeeCreatedEvent",
            "com.poc.training.shared.event.EmployeeDeactivatedEvent",
            "com.poc.training.shared.event.EmployeeUpdatedEvent"
    );

    private final ObjectMapper objectMapper;

    public Jackson3JsonMessageConverter() {
        this.objectMapper = new ObjectMapper();
    }

    public Jackson3JsonMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        if (object instanceof Message message) {
            return message;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(object);
            messageProperties.setContentType("application/json");
            messageProperties.setContentLength(bytes.length);
            return new Message(bytes, messageProperties);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert object to message", e);
        }
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        return fromMessage(message, (Object) null);
    }

    @Override
    public Object fromMessage(Message message, Object conversionHint) throws MessageConversionException {
        MessageProperties props = message.getMessageProperties();
        Object typeId = props.getHeaders().get("__TypeId__");
        Class<?> targetClass = resolveTargetClass(typeId, conversionHint);

        if (targetClass == null) {
            throw new MessageConversionException("No target class resolved. __TypeId__: " + typeId);
        }

        try {
            return objectMapper.readValue(message.getBody(), targetClass);
        } catch (Exception e) {
            log.error("Failed to convert message to object of type {}: {}", targetClass.getName(), e.getMessage(), e);
            throw new MessageConversionException("Failed to convert message to object", e);
        }
    }

    private Class<?> resolveTargetClass(Object typeId, Object conversionHint) {
        if (conversionHint instanceof Class<?> clazz) {
            return clazz;
        }
        if (typeId instanceof String className) {
            if (!ALLOWED_TYPES.contains(className)) {
                log.warn("Blocked deserialization of non-allowed type: {}", className);
                return null;
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.warn("Class not found for __TypeId__: {}", className);
                return null;
            }
        }
        return null;
    }
}

package com.poc.training.shared.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SmartMessageConverter;

public class Jackson3JsonMessageConverter implements SmartMessageConverter {

    private static final Logger log = LoggerFactory.getLogger(Jackson3JsonMessageConverter.class);
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
            log.error("Failed to convert message to object: {}", e.getMessage());
            throw new MessageConversionException("Failed to convert message to object", e);
        }
    }

    private Class<?> resolveTargetClass(Object typeId, Object conversionHint) {
        if (conversionHint instanceof Class<?> clazz) {
            return clazz;
        }
        if (typeId instanceof String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return null;
    }
}

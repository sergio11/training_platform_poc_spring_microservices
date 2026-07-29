package com.poc.training.shared.converter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Jackson3JsonMessageConverterTest {

    private final Jackson3JsonMessageConverter converter = new Jackson3JsonMessageConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SimpleObject implements Serializable {
        private String name;
        private int value;
    }

    @Test
    void toMessageShouldConvertObjectToJson() {
        SimpleObject obj = new SimpleObject("test", 42);
        MessageProperties props = new MessageProperties();

        Message message = converter.toMessage(obj, props);

        assertNotNull(message);
        assertEquals("application/json", props.getContentType());
        assertTrue(props.getContentLength() > 0);
        assertNotNull(message.getBody());
        assertTrue(message.getBody().length > 0);
    }

    @Test
    void toMessageShouldPassThroughExistingMessage() {
        MessageProperties props = new MessageProperties();
        props.setHeader("custom", "value");
        Message original = new Message("body".getBytes(), props);

        Message result = converter.toMessage(original, new MessageProperties());

        assertSame(original, result);
        assertEquals("value", result.getMessageProperties().getHeaders().get("custom"));
    }

    @Test
    void toMessageShouldWrapSerializationException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsBytes(any())).thenThrow(new JacksonException("mock error", null, null) {});
        Jackson3JsonMessageConverter failingConverter = new Jackson3JsonMessageConverter(mockMapper);

        assertThrows(MessageConversionException.class,
                () -> failingConverter.toMessage(new SimpleObject("x", 1), new MessageProperties()));
    }

    @Test
    void fromMessageShouldDeserializeUsingConversionHint() throws Exception {
        SimpleObject obj = new SimpleObject("hint", 99);
        byte[] json = objectMapper.writeValueAsBytes(obj);

        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        Message message = new Message(json, props);

        Object result = converter.fromMessage(message, SimpleObject.class);

        assertNotNull(result);
        assertInstanceOf(SimpleObject.class, result);
        SimpleObject deserialized = (SimpleObject) result;
        assertEquals("hint", deserialized.getName());
        assertEquals(99, deserialized.getValue());
    }

    @Test
    void fromMessageShouldThrowWhenNoTypeIdAndNoHint() {
        MessageProperties props = new MessageProperties();
        Message message = new Message("body".getBytes(), props);

        MessageConversionException ex = assertThrows(MessageConversionException.class,
                () -> converter.fromMessage(message));
        assertTrue(ex.getMessage().contains("No target class resolved"));
    }

    @Test
    void fromMessageShouldThrowForBlockedTypeId() {
        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", SimpleObject.class.getName());
        Message message = new Message("not json {{{".getBytes(), props);

        MessageConversionException ex = assertThrows(MessageConversionException.class,
                () -> converter.fromMessage(message));
        assertTrue(ex.getMessage().contains("No target class resolved"));
    }

    @Test
    void fromMessageShouldThrowForInvalidTypeId() {
        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", "com.nonexistent.FakeClass");
        Message message = new Message("{}".getBytes(), props);

        MessageConversionException ex = assertThrows(MessageConversionException.class,
                () -> converter.fromMessage(message));
        assertTrue(ex.getMessage().contains("No target class resolved"));
    }

    @Test
    void fromMessageShouldUseHintOverTypeIdWhenBothPresent() throws Exception {
        SimpleObject obj = new SimpleObject("both", 1);
        byte[] json = objectMapper.writeValueAsBytes(obj);

        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", "com.nonexistent.FakeClass");
        Message message = new Message(json, props);

        Object result = converter.fromMessage(message, SimpleObject.class);

        assertNotNull(result);
        assertInstanceOf(SimpleObject.class, result);
        SimpleObject deserialized = (SimpleObject) result;
        assertEquals("both", deserialized.getName());
        assertEquals(1, deserialized.getValue());
    }

    @Test
    void customObjectMapperShouldBeUsed() {
        Jackson3JsonMessageConverter customConverter = new Jackson3JsonMessageConverter(objectMapper);
        SimpleObject obj = new SimpleObject("custom", 5);
        MessageProperties props = new MessageProperties();

        Message message = customConverter.toMessage(obj, props);

        assertNotNull(message);
        assertTrue(message.getBody().length > 0);
    }

    @Test
    void fromMessageWithNullTypeIdStringShouldThrow() {
        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", 12345);
        Message message = new Message("{}".getBytes(), props);

        assertThrows(MessageConversionException.class,
                () -> converter.fromMessage(message));
    }

    @Test
    void fromMessageSingleArgShouldDelegateToTwoArg() {
        MessageProperties props = new MessageProperties();
        Message message = new Message("body".getBytes(), props);

        MessageConversionException ex = assertThrows(MessageConversionException.class,
                () -> converter.fromMessage(message));
        assertTrue(ex.getMessage().contains("No target class resolved"));
    }

    @Test
    void fromMessageShouldDeserializeUsingValidAllowedType() throws Exception {
        String json = "{\"eventId\":\"e1\",\"source\":\"test\",\"enrollmentId\":1,\"employeeId\":10,\"courseId\":20}";
        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", "com.poc.training.shared.event.EnrollmentCreatedEvent");
        Message message = new Message(json.getBytes(), props);

        Object result = converter.fromMessage(message);

        assertNotNull(result);
        assertInstanceOf(com.poc.training.shared.event.EnrollmentCreatedEvent.class, result);
        com.poc.training.shared.event.EnrollmentCreatedEvent event =
                (com.poc.training.shared.event.EnrollmentCreatedEvent) result;
        assertEquals(1L, event.getEnrollmentId());
        assertEquals(10L, event.getEmployeeId());
        assertEquals(20L, event.getCourseId());
    }

    @Test
    void fromMessageShouldThrowWhenDeserializationFailsForAllowedType() {
        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", "com.poc.training.shared.event.EnrollmentCreatedEvent");
        Message message = new Message("not valid json {{{".getBytes(), props);

        MessageConversionException ex = assertThrows(MessageConversionException.class,
                () -> converter.fromMessage(message));
        assertTrue(ex.getMessage().contains("Failed to convert message to object"));
    }
}

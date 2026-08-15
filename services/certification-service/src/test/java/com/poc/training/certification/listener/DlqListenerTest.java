package com.poc.training.certification.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class DlqListenerTest {

    @InjectMocks
    private DlqListener dlqListener;

    @Test
    void shouldHandleDlqMessage() {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("certification.dlq");
        Message message = new Message("test body".getBytes(), props);

        assertDoesNotThrow(() -> dlqListener.handleDlqMessage(message));
    }
}

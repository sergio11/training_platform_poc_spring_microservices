package com.poc.training.catalog.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class DlqListenerTest {

    @InjectMocks
    private DlqListener dlqListener;

    @Test
    void shouldHandleDlqMessage() {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("catalog.test.routing");
        Message message = new Message("test body".getBytes(), props);

        dlqListener.handleDlqMessage(message);
    }
}

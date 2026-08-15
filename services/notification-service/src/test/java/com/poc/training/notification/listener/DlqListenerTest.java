package com.poc.training.notification.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DlqListenerTest {

    @InjectMocks
    private DlqListener dlqListener;

    @Test
    void shouldHandleDlqMessage() {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("notification.test.routing");
        Message message = new Message("test body".getBytes(), props);

        dlqListener.handleDlqMessage(message);
    }
}

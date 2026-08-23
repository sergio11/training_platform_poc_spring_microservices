package com.poc.training.notification.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqListener {

    @RabbitListener(queues = "notification.dlq")
    public void handleDlqMessage(Message message) {
        log.error("[DLQ] Received failed message in notification.dlq: routingKey={}, body={}, timestamp={}",
                message.getMessageProperties().getReceivedRoutingKey(),
                new String(message.getBody()),
                java.time.LocalDateTime.now());
    }
}

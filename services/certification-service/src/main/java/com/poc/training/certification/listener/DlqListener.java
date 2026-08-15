package com.poc.training.certification.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqListener {

    @RabbitListener(queues = "certification.dlq")
    public void handleDlqMessage(Message message) {
        log.warn("[DLQ] Received failed message in certification.dlq: routingKey={}, body={}",
                message.getMessageProperties().getReceivedRoutingKey(),
                new String(message.getBody()));
    }
}

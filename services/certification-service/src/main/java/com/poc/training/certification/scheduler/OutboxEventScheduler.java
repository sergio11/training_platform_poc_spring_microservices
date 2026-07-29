package com.poc.training.certification.scheduler;

import com.poc.training.certification.model.OutboxEvent;
import com.poc.training.certification.repository.OutboxEventRepository;
import com.poc.training.shared.event.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${outbox.polling-interval:5000}")
    public void processPendingEvents() {
        var pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(
                PageRequest.of(0, 100));
        for (OutboxEvent event : pendingEvents) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    MessageProperties props = new MessageProperties();
                    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    props.setHeader("__TypeId__", resolveEventType(event.getEventType()));
                    Message message = new Message(
                            event.getPayload().getBytes(StandardCharsets.UTF_8), props);
                    rabbitTemplate.send(
                            RabbitMQConstants.EXCHANGE,
                            event.getRoutingKey(),
                            message);
                    event.setProcessed(true);
                    outboxEventRepository.save(event);
                    log.info("Sent outbox event: {} {}", event.getEventType(), event.getAggregateId());
                });
            } catch (Exception e) {
                log.error("Failed to send outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }

    private String resolveEventType(String eventType) {
        return switch (eventType) {
            case "CertificateGeneratedEvent" -> "com.poc.training.shared.event.CertificateGeneratedEvent";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
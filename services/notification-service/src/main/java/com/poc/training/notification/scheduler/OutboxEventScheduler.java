package com.poc.training.notification.scheduler;

import com.poc.training.notification.model.OutboxEvent;
import com.poc.training.notification.repository.OutboxEventRepository;
import com.poc.training.shared.event.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${outbox.polling-interval:5000}")
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pendingEvents) {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConstants.EXCHANGE,
                            event.getRoutingKey(),
                            event.getPayload());
                    event.setProcessed(true);
                    outboxEventRepository.save(event);
                    log.info("Sent outbox event: {} {}", event.getEventType(), event.getAggregateId());
                } catch (Exception e) {
                    log.error("Failed to send outbox event {}: {}", event.getId(), e.getMessage());
                    throw e;
                }
            });
        }
    }
}

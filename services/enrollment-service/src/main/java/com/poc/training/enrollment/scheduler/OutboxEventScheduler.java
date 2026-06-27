package com.poc.training.enrollment.scheduler;

import com.poc.training.enrollment.model.OutboxEvent;
import com.poc.training.enrollment.repository.OutboxEventRepository;
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

    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pendingEvents) {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    event.setProcessed(true);
                    outboxEventRepository.save(event);
                    rabbitTemplate.convertAndSend(
                            RabbitMQConstants.EXCHANGE,
                            event.getRoutingKey(),
                            event.getPayload());
                    log.info("Sent outbox event: {} {}", event.getEventType(), event.getAggregateId());
                } catch (Exception e) {
                    log.error("Failed to send outbox event {}: {}", event.getId(), e.getMessage());
                    throw e;
                }
            });
        }
    }
}

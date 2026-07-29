package com.poc.training.employee.scheduler;

import com.poc.training.employee.model.OutboxEvent;
import com.poc.training.employee.repository.OutboxEventRepository;
import com.poc.training.shared.event.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
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
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(
                PageRequest.of(0, 100));
        for (OutboxEvent event : events) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, event.getRoutingKey(), event.getPayload());
                    event.setProcessed(true);
                    outboxEventRepository.save(event);
                    log.info("Processed outbox event: {} id={}", event.getEventType(), event.getId());
                });
            } catch (Exception e) {
                log.error("Failed to process outbox event id={}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
package com.poc.training.shared.scheduler;

import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.model.OutboxEvent;
import com.poc.training.shared.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    static final int MAX_RETRIES = 5;

    private static final Map<String, String> EVENT_TYPE_MAP = Map.ofEntries(
            Map.entry("EnrollmentCreatedEvent", "com.poc.training.shared.event.EnrollmentCreatedEvent"),
            Map.entry("EnrollmentApprovedEvent", "com.poc.training.shared.event.EnrollmentApprovedEvent"),
            Map.entry("EnrollmentRejectedEvent", "com.poc.training.shared.event.EnrollmentRejectedEvent"),
            Map.entry("EnrollmentCancelledEvent", "com.poc.training.shared.event.EnrollmentCancelledEvent"),
            Map.entry("CourseCompletedEvent", "com.poc.training.shared.event.CourseCompletedEvent"),
            Map.entry("CertificateGeneratedEvent", "com.poc.training.shared.event.CertificateGeneratedEvent"),
            Map.entry("EmployeeCreatedEvent", "com.poc.training.shared.event.EmployeeCreatedEvent"),
            Map.entry("EmployeeUpdatedEvent", "com.poc.training.shared.event.EmployeeUpdatedEvent"),
            Map.entry("EmployeeDeactivatedEvent", "com.poc.training.shared.event.EmployeeDeactivatedEvent"),
            Map.entry("NotificationSentEvent", "com.poc.training.shared.event.NotificationSentEvent")
    );

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${outbox.polling-interval:5000}")
    @Transactional
    public void processPendingEvents() {
        var pendingEvents = outboxEventRepository.findPendingEventsWithLock(
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
                log.error("Failed to send outbox event {} (type={}, aggregateId={}): {}",
                        event.getId(), event.getEventType(), event.getAggregateId(), e.getMessage(), e);
                handleRetry(event);
            }
        }
    }

    private void handleRetry(OutboxEvent event) {
        try {
            int newRetryCount = event.getRetryCount() + 1;
            if (newRetryCount >= MAX_RETRIES) {
                event.setProcessed(true);
                log.error("Outbox event {} exceeded max retries ({}), marking as failed: type={}, aggregateId={}",
                        event.getId(), MAX_RETRIES, event.getEventType(), event.getAggregateId());
            } else {
                event.setRetryCount(newRetryCount);
                log.warn("Outbox event {} will retry ({}/{}): type={}, aggregateId={}",
                        event.getId(), newRetryCount, MAX_RETRIES, event.getEventType(), event.getAggregateId());
            }
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            log.error("Failed to update retry count for outbox event {}: {}", event.getId(), ex.getMessage(), ex);
        }
    }

    private String resolveEventType(String eventType) {
        String fqcn = EVENT_TYPE_MAP.get(eventType);
        if (fqcn == null) {
            throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
        return fqcn;
    }
}

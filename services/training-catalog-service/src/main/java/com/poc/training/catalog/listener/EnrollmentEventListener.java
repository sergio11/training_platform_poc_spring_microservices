package com.poc.training.catalog.listener;

import com.poc.training.catalog.model.ProcessedEvent;
import com.poc.training.catalog.repository.ProcessedEventRepository;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.shared.event.EnrollmentApprovedEvent;
import com.poc.training.shared.event.EnrollmentCancelledEvent;
import com.poc.training.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentEventListener {

    private final CourseService courseService;
    private final ProcessedEventRepository processedEventRepository;

    @RabbitListener(queues = "enrollment.approved")
    public void handleEnrollmentApproved(EnrollmentApprovedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("Event already processed: {}", event.getEventId());
            return;
        }
        try {
            courseService.incrementEnrolledCount(event.getCourseId());
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("EnrollmentApprovedEvent")
                    .build());
        } catch (BusinessException e) {
            log.warn("BusinessException processing enrollment.approved for courseId {}: {}", event.getCourseId(), e.getMessage());
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("EnrollmentApprovedEvent")
                    .build());
        } catch (Exception e) {
            log.error("Error processing enrollment.approved for courseId {}: {}", event.getCourseId(), e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "enrollment.cancelled")
    public void handleEnrollmentCancelled(EnrollmentCancelledEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.debug("Event already processed: {}", event.getEventId());
            return;
        }
        try {
            courseService.decrementEnrolledCount(event.getCourseId());
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("EnrollmentCancelledEvent")
                    .build());
        } catch (BusinessException e) {
            log.warn("BusinessException processing enrollment.cancelled for courseId {}: {}", event.getCourseId(), e.getMessage());
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .eventType("EnrollmentCancelledEvent")
                    .build());
        } catch (Exception e) {
            log.error("Error processing enrollment.cancelled for courseId {}: {}", event.getCourseId(), e.getMessage(), e);
            throw e;
        }
    }
}

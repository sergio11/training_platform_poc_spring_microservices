package com.poc.training.catalog.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventTest {

    @Test
    void shouldSetProcessedAtOnPrePersist() {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId("evt-1")
                .eventType("EnrollmentApprovedEvent")
                .build();

        event.onCreate();

        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getProcessedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldCreateWithBuilder() {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId("evt-1")
                .eventType("EnrollmentApprovedEvent")
                .processedAt(LocalDateTime.now())
                .build();

        assertThat(event.getEventId()).isEqualTo("evt-1");
        assertThat(event.getEventType()).isEqualTo("EnrollmentApprovedEvent");
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        ProcessedEvent event = new ProcessedEvent();
        assertThat(event).isNotNull();
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        ProcessedEvent event = new ProcessedEvent("evt-1", "EnrollmentApprovedEvent", LocalDateTime.now());
        assertThat(event.getEventId()).isEqualTo("evt-1");
    }
}

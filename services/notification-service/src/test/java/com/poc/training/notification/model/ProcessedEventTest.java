package com.poc.training.notification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventTest {

    @Test
    void shouldSetProcessedAtOnPrePersist() throws Exception {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId("evt-1")
                .eventType("ENROLLMENT_CREATED")
                .build();

        java.lang.reflect.Method onCreate = ProcessedEvent.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(event);

        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldCreateWithBuilder() {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId("evt-1")
                .eventType("ENROLLMENT_CREATED")
                .processedAt(LocalDateTime.now())
                .build();

        assertThat(event.getEventId()).isEqualTo("evt-1");
        assertThat(event.getEventType()).isEqualTo("ENROLLMENT_CREATED");
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        ProcessedEvent event = new ProcessedEvent();
        assertThat(event).isNotNull();
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        ProcessedEvent event = new ProcessedEvent("evt-1", "ENROLLMENT_CREATED", now);
        assertThat(event.getEventId()).isEqualTo("evt-1");
        assertThat(event.getEventType()).isEqualTo("ENROLLMENT_CREATED");
        assertThat(event.getProcessedAt()).isEqualTo(now);
    }

    @Test
    void shouldHaveCorrectToString() {
        ProcessedEvent event = ProcessedEvent.builder()
                .eventId("evt-1")
                .eventType("ENROLLMENT_CREATED")
                .build();
        String str = event.toString();
        assertThat(str).contains("evt-1");
        assertThat(str).contains("ENROLLMENT_CREATED");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        ProcessedEvent e1 = ProcessedEvent.builder().eventId("evt-1").eventType("A").build();
        ProcessedEvent e2 = ProcessedEvent.builder().eventId("evt-1").eventType("B").build();
        ProcessedEvent e3 = ProcessedEvent.builder().eventId("evt-2").eventType("A").build();

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
    }
}

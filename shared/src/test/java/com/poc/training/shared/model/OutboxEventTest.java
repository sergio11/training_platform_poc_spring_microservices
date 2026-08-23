package com.poc.training.shared.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void shouldCreateEventWithBuilder() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Enrollment")
                .aggregateId("agg-1")
                .eventType("EnrollmentCreatedEvent")
                .payload("{\"id\":1}")
                .routingKey("enrollment.created")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .processed(false)
                .build();

        assertEquals(1L, event.getId());
        assertEquals("Enrollment", event.getAggregateType());
        assertEquals("agg-1", event.getAggregateId());
        assertEquals("EnrollmentCreatedEvent", event.getEventType());
        assertEquals("{\"id\":1}", event.getPayload());
        assertEquals("enrollment.created", event.getRoutingKey());
        assertEquals(LocalDateTime.of(2026, 1, 1, 12, 0), event.getCreatedAt());
        assertFalse(event.isProcessed());
    }

    @Test
    void shouldSetProcessedTrue() {
        OutboxEvent event = OutboxEvent.builder().build();
        event.setProcessed(true);
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldUseExplicitEqualsAndHashCode() {
        OutboxEvent event1 = OutboxEvent.builder().id(1L).build();
        OutboxEvent event2 = OutboxEvent.builder().id(1L).build();
        OutboxEvent event3 = OutboxEvent.builder().id(2L).build();

        assertEquals(event1, event2);
        assertNotEquals(event1, event3);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void shouldGetAndSetVersion() {
        OutboxEvent event = OutboxEvent.builder().build();
        event.setVersion(5L);
        assertEquals(5L, event.getVersion());
    }

    @Test
    void shouldGetAndSetRetryCount() {
        OutboxEvent event = OutboxEvent.builder().build();
        event.setRetryCount(3);
        assertEquals(3, event.getRetryCount());
    }

    @Test
    void shouldCallOnPrePersist() throws Exception {
        OutboxEvent event = OutboxEvent.builder().build();
        var method = OutboxEvent.class.getDeclaredMethod("onCreate");
        method.setAccessible(true);
        method.invoke(event);

        assertNotNull(event.getCreatedAt());
        assertFalse(event.isProcessed());
    }
}

package com.poc.training.employee.model;

import com.poc.training.shared.model.OutboxEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void shouldSetDefaultsOnPrePersist() throws Exception {
        OutboxEvent event = OutboxEvent.builder().build();

        Method onCreate = OutboxEvent.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(event);

        assertNotNull(event.getCreatedAt());
        assertFalse(event.isProcessed());
    }

    @Test
    void shouldBuilderCorrectly() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateType("Employee")
                .aggregateId("100")
                .eventType("EmployeeCreatedEvent")
                .payload("{\"key\":\"value\"}")
                .routingKey("employee.created")
                .processed(true)
                .build();

        assertEquals(1L, event.getId());
        assertEquals("Employee", event.getAggregateType());
        assertEquals("100", event.getAggregateId());
        assertEquals("EmployeeCreatedEvent", event.getEventType());
        assertEquals("{\"key\":\"value\"}", event.getPayload());
        assertEquals("employee.created", event.getRoutingKey());
        assertTrue(event.isProcessed());
    }

    @Test
    void shouldHaveEqualsAndHashCodeById() {
        OutboxEvent e1 = OutboxEvent.builder().id(1L).build();
        OutboxEvent e2 = OutboxEvent.builder().id(1L).build();
        OutboxEvent e3 = OutboxEvent.builder().id(2L).build();

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        OutboxEvent event = new OutboxEvent();
        assertNull(event.getId());
        assertNull(event.getAggregateType());
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = new OutboxEvent(1L, "Employee", "100", "Created", "payload", "routing", now, false, 0L, 0);

        assertEquals(1L, event.getId());
        assertEquals("Employee", event.getAggregateType());
        assertEquals(now, event.getCreatedAt());
        assertFalse(event.isProcessed());
    }

    @Test
    void shouldSetAndGetAllFields() {
        OutboxEvent event = new OutboxEvent();
        event.setId(5L);
        event.setAggregateType("Employee");
        event.setAggregateId("50");
        event.setEventType("EmployeeDeactivatedEvent");
        event.setPayload("{}");
        event.setRoutingKey("employee.deactivated");
        event.setProcessed(true);

        assertEquals(5L, event.getId());
        assertEquals("EmployeeDeactivatedEvent", event.getEventType());
        assertTrue(event.isProcessed());
    }
}

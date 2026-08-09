package com.poc.training.certification.model;

import com.poc.training.shared.model.OutboxEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    @Test
    void shouldInitializeOnCreate() throws Exception {
        OutboxEvent event = new OutboxEvent();
        assertNull(event.getCreatedAt());
        assertFalse(event.isProcessed());

        Method onCreate = OutboxEvent.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(event);

        assertNotNull(event.getCreatedAt());
        assertFalse(event.isProcessed());
    }

    @Test
    void shouldVerifyEqualsAndHashCode() {
        OutboxEvent e1 = OutboxEvent.builder().id(1L).aggregateType("Certificate").build();
        OutboxEvent e2 = OutboxEvent.builder().id(1L).aggregateType("Other").build();
        OutboxEvent e3 = OutboxEvent.builder().id(2L).aggregateType("Certificate").build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
    }
}

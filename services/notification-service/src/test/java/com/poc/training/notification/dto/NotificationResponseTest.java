package com.poc.training.notification.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseTest {

    @Test
    void shouldSetAndGetAllFields() {
        LocalDateTime now = LocalDateTime.now();
        NotificationResponse response = NotificationResponse.builder().build();
        response.setId(1L);
        response.setType("ENROLLMENT_CREATED");
        response.setRecipientId(42L);
        response.setMessage("Test message");
        response.setReferenceId("10");
        response.setReferenceType("ENROLLMENT");
        response.setSentAt(now);
        response.setStatus("PENDING");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo("ENROLLMENT_CREATED");
        assertThat(response.getRecipientId()).isEqualTo(42L);
        assertThat(response.getMessage()).isEqualTo("Test message");
        assertThat(response.getReferenceId()).isEqualTo("10");
        assertThat(response.getReferenceType()).isEqualTo("ENROLLMENT");
        assertThat(response.getSentAt()).isEqualTo(now);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldHaveCorrectToString() {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L).type("ENROLLMENT_CREATED").build();
        String str = response.toString();
        assertThat(str).contains("ENROLLMENT_CREATED");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30);
        NotificationResponse r1 = NotificationResponse.builder()
                .id(1L).type("ENROLLMENT_CREATED").recipientId(1L)
                .message("Test").referenceId("10").referenceType("ENROLLMENT")
                .sentAt(now).status("PENDING").build();
        NotificationResponse r2 = NotificationResponse.builder()
                .id(1L).type("ENROLLMENT_CREATED").recipientId(1L)
                .message("Test").referenceId("10").referenceType("ENROLLMENT")
                .sentAt(now).status("PENDING").build();
        NotificationResponse r3 = NotificationResponse.builder()
                .id(2L).type("CERTIFICATE_GENERATED").recipientId(2L)
                .message("Other").referenceId("20").referenceType("CERTIFICATE")
                .sentAt(now).status("DELIVERED").build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(r3);
    }

    @Test
    void shouldBeInstantiableWithBuilder() {
        NotificationResponse response = NotificationResponse.builder().build();
        assertThat(response).isNotNull();
    }
}

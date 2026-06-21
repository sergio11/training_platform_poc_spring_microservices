package com.poc.training.notification.controller;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationStatus;
import com.poc.training.notification.model.NotificationType;
import com.poc.training.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void shouldReturnNotificationsByRecipientId() throws Exception {
        Notification n = Notification.builder()
                .id(1L)
                .recipientId("1")
                .type(NotificationType.ENROLLMENT_CREATED)
                .message("Test notification")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.findByRecipientId("1")).thenReturn(Arrays.asList(n));

        mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientId").value("1"));
    }

    @Test
    void shouldReturnNotificationById() throws Exception {
        Notification n = Notification.builder()
                .id(1L)
                .recipientId("1")
                .type(NotificationType.ENROLLMENT_CREATED)
                .message("Test notification")
                .status(NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.findById(1L)).thenReturn(n);

        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Test notification"));
    }
}

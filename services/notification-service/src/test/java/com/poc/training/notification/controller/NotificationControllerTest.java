package com.poc.training.notification.controller;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .recipientId("1")
                .type("ENROLLMENT_CREATED")
                .message("Test notification")
                .status("SENT")
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.findByRecipientId(eq("1"), any())).thenReturn(new PageImpl<>(Arrays.asList(response)));

        mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipientId").value("1"));
    }

    @Test
    void shouldReturnNotificationById() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .recipientId("1")
                .type("ENROLLMENT_CREATED")
                .message("Test notification")
                .status("SENT")
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Test notification"));
    }
}

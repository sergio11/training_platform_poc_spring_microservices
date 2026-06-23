package com.poc.training.notification.controller;

import com.poc.training.notification.config.SecurityConfig;
import com.poc.training.notification.service.NotificationService;
import com.poc.training.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void endpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void endpoint_shouldReturn403WithoutRole() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void endpoint_shouldReturn200WithEmployeeRole() throws Exception {
        Page<NotificationResponse> page = new PageImpl<>(List.of());
        when(notificationService.findByRecipientId(any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointById_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void endpointById_shouldReturn200WithEmployeeRole() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L).type("ENROLLMENT_CREATED").recipientId("1")
                .message("Test").referenceId("1").referenceType("ENROLLMENT")
                .sentAt(LocalDateTime.now()).status("SENT").build();
        when(notificationService.findById(any())).thenReturn(response);
        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isOk());
    }
}

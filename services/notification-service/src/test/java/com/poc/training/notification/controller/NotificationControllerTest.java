package com.poc.training.notification.controller;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.service.NotificationService;
import com.poc.training.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import com.poc.training.shared.exception.BusinessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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
                .recipientId(1L)
                .type("ENROLLMENT_CREATED")
                .message("Test notification")
                .status("SENT")
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationService.findByRecipientId(eq(1L), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(response)));

        mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipientId").value(1));
    }

    @Test
    void shouldReturnPaginatedNotifications() throws Exception {
        NotificationResponse r1 = NotificationResponse.builder()
                .id(1L).recipientId(1L).type("ENROLLMENT_CREATED").message("M1").status("SENT").build();
        NotificationResponse r2 = NotificationResponse.builder()
                .id(2L).recipientId(1L).type("CERTIFICATE_GENERATED").message("M2").status("SENT").build();

        org.springframework.data.domain.Page<NotificationResponse> page =
                new PageImpl<>(Arrays.asList(r1, r2),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2);

        when(notificationService.findByRecipientId(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("recipientId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldReturnEmptyPageWhenNoNotifications() throws Exception {
        org.springframework.data.domain.Page<NotificationResponse> emptyPage =
                new PageImpl<>(Arrays.asList(),
                        org.springframework.data.domain.PageRequest.of(0, 20), 0);

        when(notificationService.findByRecipientId(eq(999L), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("recipientId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldReturnNotificationById() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .recipientId(1L)
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

    @Test
    void shouldDenyEmployeeViewingOtherNotifications() throws Exception {
        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/notifications").param("recipientId", "2"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldDenyEmployeeViewingOtherNotificationById() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L).recipientId(2L).type("ENROLLMENT_CREATED")
                .message("Test notification").status("SENT").build();
        when(notificationService.findById(1L)).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/notifications/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldReturn400ForInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .param("recipientId", "1")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowEmployeeViewingOwnNotifications() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L).recipientId(1L).type("ENROLLMENT_CREATED")
                .message("My notification").status("SENT").build();
        when(notificationService.findByRecipientId(eq(1L), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(response)));

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/notifications").param("recipientId", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldAllowEmployeeViewingOwnNotificationById() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L).recipientId(1L).type("ENROLLMENT_CREATED")
                .message("My notification").status("SENT").build();
        when(notificationService.findById(1L)).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/notifications/1"))
                    .andExpect(status().isOk());
        }
    }
}

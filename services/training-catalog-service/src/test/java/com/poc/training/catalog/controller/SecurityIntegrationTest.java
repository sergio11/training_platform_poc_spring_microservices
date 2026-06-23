package com.poc.training.catalog.controller;

import com.poc.training.catalog.config.SecurityConfig;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.catalog.dto.CourseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEndpoint_shouldReturn200WithEmployeeRole() throws Exception {
        Page<CourseResponse> page = new PageImpl<>(List.of());
        when(courseService.findAll(any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/courses")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isOk());
    }

    @Test
    void createEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"instructor\":\"X\",\"capacity\":1}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 401 || status == 403,
                            "Expected 401 or 403 but was " + status);
                });
    }

    @Test
    void createEndpoint_shouldReturn201WithAdminRole() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L).title("Test").instructor("X").capacity(1).enrolledCount(0).status("ACTIVE").build();
        when(courseService.create(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/courses")
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"instructor\":\"X\",\"capacity\":1}")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isCreated());
    }

    @Test
    void createEndpoint_shouldReturn403WithEmployeeRole() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"instructor\":\"X\",\"capacity\":1}")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 401 || status == 403,
                            "Expected 401 or 403 but was " + status);
                });
    }

    @Test
    void deleteEndpoint_shouldReturn204WithAdminRole() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEndpoint_shouldReturn403WithEmployeeRole() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }
}

package com.poc.training.catalog.controller;

import com.poc.training.catalog.config.SecurityConfig;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.catalog.dto.CourseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void publicEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"instructor\":\"X\",\"capacity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getEndpoint_shouldReturn403WithoutRole() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void getEndpoint_shouldReturn200WithEmployeeRole() throws Exception {
        Page<CourseResponse> page = new PageImpl<>(List.of());
        when(courseService.findAll(any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"admin"})
    void createEndpoint_shouldReturn201WithAdminRole() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L).title("Test").instructor("X").capacity(1).enrolledCount(0).status("ACTIVE").build();
        when(courseService.create(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/courses")
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"instructor\":\"X\",\"capacity\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void createEndpoint_shouldReturn403WithEmployeeRole() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"instructor\":\"X\",\"capacity\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedDeleteEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"admin"})
    void protectedDeleteEndpoint_shouldReturn200WithAdminRole() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void protectedDeleteEndpoint_shouldReturn403WithEmployeeRole() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isForbidden());
    }
}

package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.config.SecurityConfig;
import com.poc.training.enrollment.service.EnrollmentService;
import com.poc.training.enrollment.dto.EnrollmentResponse;
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

@WebMvcTest(controllers = EnrollmentController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void endpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void endpoint_shouldReturn403WithoutRole() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void endpoint_shouldReturn200WithEmployeeRole() throws Exception {
        Page<EnrollmentResponse> page = new PageImpl<>(List.of());
        when(enrollmentService.findByEmployeeId(any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void postEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments").param("employeeId", "1").param("courseId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void postEndpoint_shouldReturn201WithEmployeeRole() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").build();
        when(enrollmentService.enroll(any(), any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/enrollments").param("employeeId", "1").param("courseId", "1"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"admin"})
    void approveEndpoint_shouldReturn200WithAdminRole() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("APPROVED").build();
        when(enrollmentService.approve(any())).thenReturn(response);
        mockMvc.perform(patch("/api/v1/enrollments/1/approve"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"employee"})
    void approveEndpoint_shouldReturn403WithEmployeeRole() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/1/approve"))
                .andExpect(status().isForbidden());
    }
}

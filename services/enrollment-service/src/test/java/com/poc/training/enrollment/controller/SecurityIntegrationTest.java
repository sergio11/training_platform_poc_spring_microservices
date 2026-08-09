package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.config.SecurityConfig;
import com.poc.training.enrollment.service.EnrollmentService;
import com.poc.training.enrollment.dto.EnrollmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EnrollmentController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void endpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpoint_shouldReturn200WithEmployeeRole() throws Exception {
        Page<EnrollmentResponse> page = new PageImpl<>(List.of());
        when(enrollmentService.findByEmployeeId(any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1")
                        .with(jwt()
                                .jwt(j -> j
                                        .claim("employeeId", "1")
                                        .claim("realm_access", Map.of("roles", List.of("employee"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isOk());
    }

    @Test
    void postEndpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":1,\"courseId\":1}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 401 || status == 403,
                            "Expected 401 or 403 but was " + status);
                });
    }

    @Test
    void postEndpoint_shouldReturn201WithEmployeeRole() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").build();
        when(enrollmentService.enroll(any(), any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":1,\"courseId\":1}")
                        .with(jwt()
                                .jwt(j -> j
                                        .claim("employeeId", "1")
                                        .claim("realm_access", Map.of("roles", List.of("employee"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isCreated());
    }

    @Test
    void approveEndpoint_shouldReturn200WithAdminRole() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("APPROVED").build();
        when(enrollmentService.approve(any())).thenReturn(response);
        mockMvc.perform(patch("/api/v1/enrollments/1/approve")
                        .with(jwt()
                                .jwt(j -> j.claim("realm_access", Map.of("roles", List.of("admin"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk());
    }

    @Test
    void approveEndpoint_shouldReturn403WithEmployeeRole() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollments/1/approve")
                        .with(jwt()
                                .jwt(j -> j.claim("realm_access", Map.of("roles", List.of("employee"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }
}

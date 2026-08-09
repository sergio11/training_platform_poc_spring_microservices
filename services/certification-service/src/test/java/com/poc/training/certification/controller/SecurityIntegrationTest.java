package com.poc.training.certification.controller;

import com.poc.training.certification.config.SecurityConfig;
import com.poc.training.certification.service.CertificationService;
import com.poc.training.certification.dto.CertificateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CertificateController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificationService certificationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void endpoint_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/certificates").param("employeeId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpoint_shouldReturn200WithEmployeeRole() throws Exception {
        Page<CertificateResponse> page = new PageImpl<>(List.of());
        when(certificationService.findByEmployeeId(any(), any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/certificates").param("employeeId", "1")
                        .with(jwt()
                                .jwt(j -> j
                                        .subject("1")
                                        .claim("employeeId", "1")
                                        .claim("realm_access", Map.of("roles", List.of("employee"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isOk());
    }

    @Test
    void endpointById_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointById_shouldReturn200WithEmployeeRole() throws Exception {
        CertificateResponse response = CertificateResponse.builder()
                .id(1L).certificateNumber("CERT-12345678").enrollmentId(1L)
                .employeeId(1L).courseId(1L).issuedAt(LocalDateTime.now()).build();
        when(certificationService.findById(any())).thenReturn(response);
        mockMvc.perform(get("/api/v1/certificates/1")
                        .with(jwt()
                                .jwt(j -> j
                                        .subject("1")
                                        .claim("employeeId", "1")
                                        .claim("realm_access", Map.of("roles", List.of("employee"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isOk());
    }
}

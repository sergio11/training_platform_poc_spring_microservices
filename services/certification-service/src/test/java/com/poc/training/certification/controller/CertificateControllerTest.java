package com.poc.training.certification.controller;

import com.poc.training.certification.dto.CertificateResponse;
import com.poc.training.certification.service.CertificationService;
import com.poc.training.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificationService certificationService;

    @Test
    void shouldReturnCertificatesByEmployeeId() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-001")
                .issuedAt(LocalDateTime.now())
                .build();

        when(certificationService.findByEmployeeId(eq(1L), any())).thenReturn(new PageImpl<>(Arrays.asList(c)));

        mockMvc.perform(get("/api/v1/certificates").param("employeeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].certificateNumber").value("CERT-001"));
    }

    @Test
    void shouldReturnPaginatedCertificates() throws Exception {
        CertificateResponse c1 = CertificateResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        CertificateResponse c2 = CertificateResponse.builder()
                .id(2L).employeeId(1L).courseId(2L).certificateNumber("CERT-002").issuedAt(LocalDateTime.now()).build();

        org.springframework.data.domain.Page<CertificateResponse> page =
                new PageImpl<>(Arrays.asList(c1, c2),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2);

        when(certificationService.findByEmployeeId(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/certificates")
                        .param("employeeId", "1")
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
    void shouldReturnEmptyPageWhenNoCertificates() throws Exception {
        org.springframework.data.domain.Page<CertificateResponse> emptyPage =
                new PageImpl<>(Arrays.asList(),
                        org.springframework.data.domain.PageRequest.of(0, 20), 0);

        when(certificationService.findByEmployeeId(eq(999L), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/certificates")
                        .param("employeeId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldReturnCertificateById() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-001")
                .issuedAt(LocalDateTime.now())
                .build();

        when(certificationService.findById(1L)).thenReturn(c);

        mockMvc.perform(get("/api/v1/certificates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateNumber").value("CERT-001"));
    }

    @Test
    void shouldReturnCertificateByNumber() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-001")
                .issuedAt(LocalDateTime.now())
                .build();

        when(certificationService.findByCertificateNumber("CERT-001")).thenReturn(c);

        mockMvc.perform(get("/api/v1/certificates/number/CERT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateNumber").value("CERT-001"));
    }

    @Test
    void shouldDenyEmployeeViewingOtherCertificates() throws Exception {
        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/certificates").param("employeeId", "2"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldAllowEmployeeToViewOwnCertificates() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findByEmployeeId(eq(1L), any())).thenReturn(new PageImpl<>(Arrays.asList(c)));

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/certificates").param("employeeId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].certificateNumber").value("CERT-001"));
        }
    }

    @Test
    void shouldDenyEmployeeViewingOtherCertificateById() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findById(1L)).thenReturn(c);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/certificates/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldDenyEmployeeViewingOtherCertificateByNumber() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findByCertificateNumber("CERT-001")).thenReturn(c);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/certificates/number/CERT-001"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldReturn400WhenSortByIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/certificates")
                        .param("employeeId", "1")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAdminToViewAnyEmployeeCertificates() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findByEmployeeId(eq(2L), any())).thenReturn(new PageImpl<>(Arrays.asList(c)));

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            // Admin is NOT employee-only: the isEmployeeOnly branch returns false
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(false);

            mockMvc.perform(get("/api/v1/certificates").param("employeeId", "2"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldAllowAdminToViewAnyEmployeeCertificateById() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findById(1L)).thenReturn(c);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            // Admin is NOT employee-only: the isEmployeeOnly branch returns false
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(false);

            mockMvc.perform(get("/api/v1/certificates/1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldAllowAdminToViewAnyEmployeeCertificateByNumber() throws Exception {
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findByCertificateNumber("CERT-001")).thenReturn(c);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            // Admin is NOT employee-only: the isEmployeeOnly branch returns false
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(false);

            mockMvc.perform(get("/api/v1/certificates/number/CERT-001"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldAllowEmployeeToViewOwnCertificateById() throws Exception {
        // Branch: isEmployeeOnly=true AND employeeId matches -> no AccessDeniedException
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findById(1L)).thenReturn(c);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/certificates/1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldAllowEmployeeToViewOwnCertificateByNumber() throws Exception {
        // Branch: isEmployeeOnly=true AND employeeId matches -> no AccessDeniedException
        CertificateResponse c = CertificateResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).certificateNumber("CERT-001").issuedAt(LocalDateTime.now()).build();
        when(certificationService.findByCertificateNumber("CERT-001")).thenReturn(c);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/certificates/number/CERT-001"))
                    .andExpect(status().isOk());
        }
    }
}


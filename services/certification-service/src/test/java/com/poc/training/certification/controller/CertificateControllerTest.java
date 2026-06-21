package com.poc.training.certification.controller;

import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.service.CertificationService;
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
        Certificate c = Certificate.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-001")
                .issuedAt(LocalDateTime.now())
                .build();

        when(certificationService.findByEmployeeId(1L)).thenReturn(Arrays.asList(c));

        mockMvc.perform(get("/api/v1/certificates").param("employeeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].certificateNumber").value("CERT-001"));
    }

    @Test
    void shouldReturnCertificateById() throws Exception {
        Certificate c = Certificate.builder()
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
        Certificate c = Certificate.builder()
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
}

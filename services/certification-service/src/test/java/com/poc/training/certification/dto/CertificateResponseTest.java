package com.poc.training.certification.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CertificateResponseTest {

    @Test
    void shouldSetAndGetAllFields() {
        CertificateResponse response = CertificateResponse.builder().build();
        response.setId(1L);
        response.setCertificateNumber("CERT-001");
        response.setEnrollmentId(10L);
        response.setEmployeeId(20L);
        response.setCourseId(30L);
        response.setIssuedAt(LocalDateTime.now());

        assertEquals(1L, response.getId());
        assertEquals("CERT-001", response.getCertificateNumber());
        assertEquals(10L, response.getEnrollmentId());
        assertEquals(20L, response.getEmployeeId());
        assertEquals(30L, response.getCourseId());
        assertNotNull(response.getIssuedAt());
    }

    @Test
    void shouldHaveCorrectToString() {
        CertificateResponse response = CertificateResponse.builder()
                .id(1L).certificateNumber("CERT-001").build();
        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("CERT-001"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30);
        CertificateResponse r1 = CertificateResponse.builder()
                .id(1L).certificateNumber("CERT-001").employeeId(1L)
                .enrollmentId(10L).courseId(20L).issuedAt(now).build();
        CertificateResponse r2 = CertificateResponse.builder()
                .id(1L).certificateNumber("CERT-001").employeeId(1L)
                .enrollmentId(10L).courseId(20L).issuedAt(now).build();
        CertificateResponse r3 = CertificateResponse.builder()
                .id(2L).certificateNumber("CERT-002").employeeId(2L)
                .enrollmentId(20L).courseId(30L).issuedAt(now).build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }
}

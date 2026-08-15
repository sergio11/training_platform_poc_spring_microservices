package com.poc.training.certification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CertificateTest {

    @Test
    void shouldSetIssuedAtOnPrePersist() {
        Certificate cert = Certificate.builder()
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        cert.onCreate();

        assertNotNull(cert.getIssuedAt());
    }

    @Test
    void shouldCreateCertificateWithBuilder() {
        Certificate cert = Certificate.builder()
                .id(1L)
                .certificateNumber("CERT-TEST-123")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .issuedAt(LocalDateTime.now())
                .build();

        assertEquals(1L, cert.getId());
        assertEquals("CERT-TEST-123", cert.getCertificateNumber());
        assertEquals(1L, cert.getEnrollmentId());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        Certificate cert = new Certificate();
        assertNotNull(cert);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        Certificate cert = new Certificate(1L, "CERT-001", 1L, 1L, 1L, LocalDateTime.now());
        assertEquals("CERT-001", cert.getCertificateNumber());
    }

    @Test
    void shouldHaveCorrectToString() {
        Certificate cert = Certificate.builder()
                .id(1L)
                .certificateNumber("CERT-TEST-123")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();
        String str = cert.toString();
        assertNotNull(str);
        assertTrue(str.contains("CERT-TEST-123"));
    }
}

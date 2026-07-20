package com.poc.training.certification.integration;

import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.certification.service.CertificationService;
import com.poc.training.shared.event.CertificateGeneratedEvent;
import com.poc.training.shared.event.CourseCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class CertificationFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("training_certification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CertificationService certificationService;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldCreateCertificate() {
        Certificate certificate = Certificate.builder()
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        Certificate saved = certificateRepository.save(certificate);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCertificateNumber());
        assertTrue(saved.getCertificateNumber().startsWith("CERT-"));
        assertNotNull(saved.getIssuedAt());
    }

    @Test
    void shouldFindCertificateByEnrollmentId() {
        Certificate certificate = Certificate.builder()
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        certificateRepository.save(certificate);

        var found = certificateRepository.findByEnrollmentId(1L);
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getEnrollmentId());
    }

    @Test
    void shouldFindCertificatesByEmployeeId() {
        Certificate cert1 = Certificate.builder()
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        Certificate cert2 = Certificate.builder()
                .enrollmentId(2L)
                .employeeId(1L)
                .courseId(2L)
                .build();

        certificateRepository.save(cert1);
        certificateRepository.save(cert2);

        var certificates = certificateRepository.findByEmployeeId(1L, PageRequest.of(0, 10));
        assertEquals(2, certificates.getContent().size());
    }

    @Test
    void shouldFindCertificateByNumber() {
        Certificate certificate = Certificate.builder()
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-TEST-12345")
                .build();

        certificateRepository.save(certificate);

        var found = certificateRepository.findByCertificateNumber("CERT-TEST-12345");
        assertTrue(found.isPresent());
        assertEquals("CERT-TEST-12345", found.get().getCertificateNumber());
    }

    @Test
    void shouldHandleCourseCompletedEvent() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(10L)
                .employeeId(10L)
                .courseId(10L)
                .build();

        certificationService.handleCourseCompleted(event);

        var cert = certificateRepository.findByEnrollmentId(10L);
        assertTrue(cert.isPresent());
        assertEquals(10L, cert.get().getEmployeeId());
        assertNotNull(cert.get().getCertificateNumber());
    }

    @Test
    void shouldCreateCertificateGeneratedEvent() {
        CertificateGeneratedEvent event = CertificateGeneratedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("certification-service")
                .certificateId(1L)
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .certificateNumber("CERT-12345")
                .build();

        assertNotNull(event.getEventId());
        assertEquals("certification-service", event.getSource());
        assertEquals("CERT-12345", event.getCertificateNumber());
    }

    @Test
    void shouldNotDuplicateCertificateForSameEnrollment() {
        Certificate cert1 = Certificate.builder()
                .enrollmentId(100L)
                .employeeId(1L)
                .courseId(1L)
                .build();
        certificateRepository.save(cert1);

        var existing = certificateRepository.findByEnrollmentId(100L);
        assertTrue(existing.isPresent());

        Certificate cert2 = Certificate.builder()
                .enrollmentId(100L)
                .employeeId(2L)
                .courseId(2L)
                .build();

        assertThrows(Exception.class, () -> certificateRepository.save(cert2));
    }
}

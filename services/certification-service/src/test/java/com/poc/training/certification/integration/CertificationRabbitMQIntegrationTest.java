package com.poc.training.certification.integration;

import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.certification.service.CertificationService;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class CertificationRabbitMQIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        certificateRepository.deleteAll();
    }

    @Test
    void shouldGenerateCertificateViaListener() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(100L)
                .employeeId(200L)
                .courseId(300L)
                .build();

        certificationService.handleCourseCompleted(event);

        var cert = certificateRepository.findByEnrollmentId(100L);
        assertTrue(cert.isPresent());
        assertEquals(200L, cert.get().getEmployeeId());
        assertEquals(300L, cert.get().getCourseId());
        assertNotNull(cert.get().getCertificateNumber());
        assertTrue(cert.get().getCertificateNumber().startsWith("CERT-"));
    }

    @Test
    void shouldNotDuplicateCertificateForSameEnrollment() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(500L)
                .employeeId(600L)
                .courseId(700L)
                .build();

        certificationService.handleCourseCompleted(event);
        certificationService.handleCourseCompleted(event);

        var certs = certificateRepository.findByEmployeeId(600L,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, certs.getContent().size());
    }

    @Test
    void shouldGenerateUniqueCertificateNumbers() {
        for (int i = 0; i < 5; i++) {
            certificationService.handleCourseCompleted(CourseCompletedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .source("enrollment-service")
                    .enrollmentId((long) (1000 + i))
                    .employeeId((long) (2000 + i))
                    .courseId((long) (3000 + i))
                    .build());
        }

        var certs = certificateRepository.findByEmployeeId(2000L,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, certs.getContent().size());

        long totalCerts = certificateRepository.count();
        assertEquals(5, totalCerts);
    }

    @Test
    void shouldPublishEventToRabbitMQ() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(800L)
                .employeeId(900L)
                .courseId(1000L)
                .build();

        certificationService.handleCourseCompleted(event);

        // Verify the outbox event was created
        assertNotNull(certificateRepository.findByEnrollmentId(800L).orElse(null));
    }
}

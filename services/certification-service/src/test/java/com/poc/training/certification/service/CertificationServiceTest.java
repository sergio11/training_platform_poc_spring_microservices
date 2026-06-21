package com.poc.training.certification.service;

import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.shared.event.CertificateGeneratedEvent;
import com.poc.training.shared.event.CourseCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CertificationService certificationService;

    @Test
    void shouldFindCertificatesByEmployeeId() {
        Certificate c1 = Certificate.builder().id(1L).employeeId(1L).certificateNumber("CERT-001").build();
        Certificate c2 = Certificate.builder().id(2L).employeeId(1L).certificateNumber("CERT-002").build();
        when(certificateRepository.findByEmployeeId(1L)).thenReturn(Arrays.asList(c1, c2));

        List<Certificate> result = certificationService.findByEmployeeId(1L);
        assertEquals(2, result.size());
    }

    @Test
    void shouldFindCertificateById() {
        Certificate c = Certificate.builder().id(1L).employeeId(1L).certificateNumber("CERT-001").build();
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(c));

        Certificate result = certificationService.findById(1L);
        assertNotNull(result);
        assertEquals("CERT-001", result.getCertificateNumber());
    }

    @Test
    void shouldThrowExceptionWhenCertificateNotFound() {
        when(certificateRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> certificationService.findById(99L));
    }

    @Test
    void shouldFindCertificateByNumber() {
        Certificate c = Certificate.builder().id(1L).certificateNumber("CERT-001").build();
        when(certificateRepository.findByCertificateNumber("CERT-001")).thenReturn(Optional.of(c));

        Certificate result = certificationService.findByCertificateNumber("CERT-001");
        assertEquals("CERT-001", result.getCertificateNumber());
    }

    @Test
    void shouldThrowExceptionWhenCertificateNumberNotFound() {
        when(certificateRepository.findByCertificateNumber("INVALID")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> certificationService.findByCertificateNumber("INVALID"));
    }

    @Test
    void shouldGenerateCertificateOnCourseCompleted() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> {
            Certificate c = inv.getArgument(0);
            c.setId(1L);
            c.setCertificateNumber("CERT-TEST-12345");
            return c;
        });

        certificationService.handleCourseCompleted(event);

        verify(certificateRepository).save(any(Certificate.class));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("certificate.generated"), any(CertificateGeneratedEvent.class));
    }

    @Test
    void shouldNotGenerateDuplicateCertificate() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        Certificate existing = Certificate.builder().id(1L).enrollmentId(1L).build();
        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.of(existing));

        certificationService.handleCourseCompleted(event);

        verify(certificateRepository, never()).save(any(Certificate.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void shouldRetryOnCertificateNumberCollision() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate certificate number"))
                .thenAnswer(inv -> {
                    Certificate c = inv.getArgument(0);
                    c.setId(1L);
                    c.setCertificateNumber("CERT-RETRY-12345");
                    return c;
                });

        certificationService.handleCourseCompleted(event);

        verify(certificateRepository, times(2)).save(any(Certificate.class));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("certificate.generated"), any(CertificateGeneratedEvent.class));
    }

    @Test
    void shouldThrowAfterMaxRetriesOnCollision() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate certificate number"));

        assertThrows(RuntimeException.class, () -> certificationService.handleCourseCompleted(event));
        verify(certificateRepository, times(3)).save(any(Certificate.class));
    }
}

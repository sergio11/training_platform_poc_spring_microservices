package com.poc.training.certification.service;

import tools.jackson.databind.ObjectMapper;
import com.poc.training.certification.dto.CertificateResponse;
import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.model.OutboxEvent;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.certification.repository.OutboxEventRepository;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    @InjectMocks
    private CertificationService certificationService;

    @Test
    void shouldFindCertificatesByEmployeeId() {
        Certificate c1 = Certificate.builder().id(1L).employeeId(1L).certificateNumber("CERT-001").build();
        Certificate c2 = Certificate.builder().id(2L).employeeId(1L).certificateNumber("CERT-002").build();
        when(certificateRepository.findByEmployeeId(eq(1L), any(PageRequest.class))).thenReturn(new PageImpl<>(Arrays.asList(c1, c2)));

        var result = certificationService.findByEmployeeId(1L, PageRequest.of(0, 10));
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldFindCertificateById() {
        Certificate c = Certificate.builder().id(1L).employeeId(1L).certificateNumber("CERT-001").build();
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(c));

        CertificateResponse result = certificationService.findById(1L);
        assertNotNull(result);
        assertEquals("CERT-001", result.getCertificateNumber());
    }

    @Test
    void shouldThrowExceptionWhenCertificateNotFound() {
        when(certificateRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> certificationService.findById(99L));
    }

    @Test
    void shouldFindCertificateByNumber() {
        Certificate c = Certificate.builder().id(1L).certificateNumber("CERT-001").build();
        when(certificateRepository.findByCertificateNumber("CERT-001")).thenReturn(Optional.of(c));

        CertificateResponse result = certificationService.findByCertificateNumber("CERT-001");
        assertEquals("CERT-001", result.getCertificateNumber());
    }

    @Test
    void shouldThrowExceptionWhenCertificateNumberNotFound() {
        when(certificateRepository.findByCertificateNumber("INVALID")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> certificationService.findByCertificateNumber("INVALID"));
    }

    @Test
    void shouldGenerateCertificateOnCourseCompleted() throws Exception {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.nextCertificateNumber()).thenReturn(1L);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> {
            Certificate c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        certificationService.handleCourseCompleted(event);

        verify(certificateRepository).save(any(Certificate.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
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
    }

    @Test
    void shouldNotThrowWhenSerializationFails() throws Exception {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.nextCertificateNumber()).thenReturn(1L);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(inv -> {
            Certificate c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(objectMapper.writeValueAsString(any())).thenThrow(new tools.jackson.core.JacksonException("serialize error", null, null) {});

        assertDoesNotThrow(() -> certificationService.handleCourseCompleted(event));
        verify(certificateRepository).save(any(Certificate.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowOnDataIntegrityViolation() {
        CourseCompletedEvent event = CourseCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("enrollment-service")
                .enrollmentId(1L)
                .employeeId(1L)
                .courseId(1L)
                .build();

        when(certificateRepository.findByEnrollmentId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.nextCertificateNumber()).thenReturn(1L);
        when(certificateRepository.save(any(Certificate.class)))
                .thenThrow(new DataIntegrityViolationException("Detail: Key (enrollment_id)=(1) already exists."));

        assertThrows(DataIntegrityViolationException.class, () ->
            certificationService.handleCourseCompleted(event)
        );
        verify(certificateRepository, times(1)).save(any(Certificate.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}

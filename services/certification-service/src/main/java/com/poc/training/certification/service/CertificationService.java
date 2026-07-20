package com.poc.training.certification.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.poc.training.certification.dto.CertificateResponse;
import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.model.OutboxEvent;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.certification.repository.OutboxEventRepository;
import com.poc.training.shared.event.CertificateGeneratedEvent;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificateRepository certificateRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public static CertificateResponse toResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .certificateNumber(certificate.getCertificateNumber())
                .enrollmentId(certificate.getEnrollmentId())
                .employeeId(certificate.getEmployeeId())
                .courseId(certificate.getCourseId())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CertificateResponse> findByEmployeeId(Long employeeId, Pageable pageable) {
        return certificateRepository.findByEmployeeId(employeeId, pageable).map(CertificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public CertificateResponse findById(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));
        return toResponse(certificate);
    }

    @Transactional(readOnly = true)
    public CertificateResponse findByCertificateNumber(String certificateNumber) {
        Certificate certificate = certificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with number: " + certificateNumber));
        return toResponse(certificate);
    }

    @Transactional
    @RabbitListener(queues = "course.completed")
    public void handleCourseCompleted(CourseCompletedEvent event) {
        log.info("Received CourseCompletedEvent for enrollment: {}", event.getEnrollmentId());

        if (certificateRepository.findByEnrollmentId(event.getEnrollmentId()).isPresent()) {
            log.info("Certificate already exists for enrollment: {}", event.getEnrollmentId());
            return;
        }

        Certificate saved = generateCertificate(event);

        try {
            CertificateGeneratedEvent certificateEvent = CertificateGeneratedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .source("certification-service")
                    .certificateId(saved.getId())
                    .enrollmentId(saved.getEnrollmentId())
                    .employeeId(saved.getEmployeeId())
                    .courseId(saved.getCourseId())
                    .certificateNumber(saved.getCertificateNumber())
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Certificate")
                    .aggregateId(String.valueOf(saved.getId()))
                    .eventType("CertificateGeneratedEvent")
                    .payload(objectMapper.writeValueAsString(certificateEvent))
                    .routingKey(RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Persisted CertificateGeneratedEvent to outbox for certificate: {}", saved.getId());
        } catch (JacksonException e) {
            log.error("Failed to serialize CertificateGeneratedEvent: {}", e.getMessage());
        }
    }

    private Certificate generateCertificate(CourseCompletedEvent event) {
        try {
            String seq = String.format("%06d", certificateRepository.nextCertificateNumber());
            String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
            String certificateNumber = "CERT-" + date + "-" + seq;

            Certificate certificate = Certificate.builder()
                    .certificateNumber(certificateNumber)
                    .enrollmentId(event.getEnrollmentId())
                    .employeeId(event.getEmployeeId())
                    .courseId(event.getCourseId())
                    .build();

            return certificateRepository.save(certificate);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation generating certificate for enrollment {}: {}",
                    event.getEnrollmentId(), e.getMessage());
            throw e;
        }
    }
}

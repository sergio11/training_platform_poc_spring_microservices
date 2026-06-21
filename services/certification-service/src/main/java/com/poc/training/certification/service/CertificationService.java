package com.poc.training.certification.service;

import com.poc.training.certification.dto.CertificateResponse;
import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.shared.event.CertificateGeneratedEvent;
import com.poc.training.shared.event.CourseCompletedEvent;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificateRepository certificateRepository;
    private final RabbitTemplate rabbitTemplate;

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

        certificateRepository.findByEnrollmentId(event.getEnrollmentId())
                .ifPresentOrElse(
                        cert -> log.info("Certificate already exists for enrollment: {}", event.getEnrollmentId()),
                        () -> generateCertificate(event)
                );
    }

    private void generateCertificate(CourseCompletedEvent event) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                Certificate certificate = Certificate.builder()
                        .enrollmentId(event.getEnrollmentId())
                        .employeeId(event.getEmployeeId())
                        .courseId(event.getCourseId())
                        .build();

                Certificate saved = certificateRepository.save(certificate);

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

                rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE, RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GENERATED, certificateEvent);
                log.info("Published CertificateGeneratedEvent for certificate: {}", saved.getId());
                return;
            } catch (DataIntegrityViolationException e) {
                log.warn("Certificate number collision on attempt {}/{} for enrollment: {}",
                        i + 1, maxRetries, event.getEnrollmentId());
                if (i == maxRetries - 1) {
                    throw new BusinessException("Failed to generate unique certificate number after " + maxRetries + " attempts");
                }
            }
        }
    }
}

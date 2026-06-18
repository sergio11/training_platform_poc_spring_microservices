package com.poc.training.certification.service;

import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.repository.CertificateRepository;
import com.poc.training.shared.event.CertificateGeneratedEvent;
import com.poc.training.shared.event.CourseCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CertificationService {

    private final CertificateRepository certificateRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "training.exchange";

    public List<Certificate> findByEmployeeId(Long employeeId) {
        return certificateRepository.findByEmployeeId(employeeId);
    }

    public Certificate findById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Certificate not found with id: " + id));
    }

    public Certificate findByCertificateNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new RuntimeException("Certificate not found with number: " + certificateNumber));
    }

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

        rabbitTemplate.convertAndSend(EXCHANGE, "certificate.generated", certificateEvent);
        log.info("Published CertificateGeneratedEvent for certificate: {}", saved.getId());
    }
}

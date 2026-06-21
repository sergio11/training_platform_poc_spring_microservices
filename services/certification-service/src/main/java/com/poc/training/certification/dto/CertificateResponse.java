package com.poc.training.certification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateResponse {
    private Long id;
    private String certificateNumber;
    private Long enrollmentId;
    private Long employeeId;
    private Long courseId;
    private LocalDateTime issuedAt;
}

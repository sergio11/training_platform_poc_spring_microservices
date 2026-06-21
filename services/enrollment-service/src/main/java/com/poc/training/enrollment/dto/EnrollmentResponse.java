package com.poc.training.enrollment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EnrollmentResponse {
    private Long id;
    private Long employeeId;
    private Long courseId;
    private String status;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}

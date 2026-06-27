package com.poc.training.employee.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String department;
    private String position;
    private String keycloakUserId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

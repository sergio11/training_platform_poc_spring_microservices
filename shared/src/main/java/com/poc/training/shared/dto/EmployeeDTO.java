package com.poc.training.shared.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String department;
    private String position;
    private boolean active;
}

package com.poc.training.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank
    private String employeeNumber;

    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    private String department;
    private String position;
    private String keycloakUserId;
}

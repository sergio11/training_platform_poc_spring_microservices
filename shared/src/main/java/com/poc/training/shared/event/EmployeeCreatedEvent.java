package com.poc.training.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmployeeCreatedEvent extends BaseEvent {
    private Long employeeId;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String department;
}

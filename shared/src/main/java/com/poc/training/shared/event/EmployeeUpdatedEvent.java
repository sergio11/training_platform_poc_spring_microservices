package com.poc.training.shared.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeUpdatedEvent extends BaseEvent {
    private Long employeeId;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String department;
    private String position;
}
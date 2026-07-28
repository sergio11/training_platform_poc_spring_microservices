package com.poc.training.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmployeeDeactivatedEvent extends BaseEvent {
    private Long employeeId;
    private String employeeNumber;
}

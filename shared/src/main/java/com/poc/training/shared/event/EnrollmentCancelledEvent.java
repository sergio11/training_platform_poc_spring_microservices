package com.poc.training.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnrollmentCancelledEvent extends BaseEvent {
    private Long enrollmentId;
    private Long employeeId;
    private Long courseId;
}

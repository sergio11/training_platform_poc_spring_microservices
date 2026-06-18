package com.poc.training.enrollment.client;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private String instructor;
    private Integer capacity;
    private Integer enrolledCount;
    private String status;
}

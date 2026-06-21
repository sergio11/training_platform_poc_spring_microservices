package com.poc.training.catalog.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private String instructor;
    private Integer capacity;
    private Integer enrolledCount;
    private String status;
}

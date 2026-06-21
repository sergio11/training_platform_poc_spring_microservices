package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<Page<EnrollmentResponse>> getEnrollmentsByEmployeeId(
            @RequestParam Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        return ResponseEntity.ok(enrollmentService.findByEmployeeId(employeeId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(@RequestParam Long employeeId, @RequestParam Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(employeeId, courseId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<EnrollmentResponse> approveEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.approve(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.complete(id));
    }
}

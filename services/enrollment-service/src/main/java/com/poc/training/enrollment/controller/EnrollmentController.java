package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public ResponseEntity<List<Enrollment>> getEnrollmentsByEmployeeId(@RequestParam Long employeeId) {
        return ResponseEntity.ok(enrollmentService.findByEmployeeId(employeeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Enrollment> enroll(@RequestParam Long employeeId, @RequestParam Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(employeeId, courseId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Enrollment> approveEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.approve(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Enrollment> completeEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.complete(id));
    }
}

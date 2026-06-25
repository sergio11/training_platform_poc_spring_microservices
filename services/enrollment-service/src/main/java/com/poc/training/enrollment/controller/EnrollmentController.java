package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.dto.EnrollmentRequest;
import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.service.EnrollmentService;
import com.poc.training.shared.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<EnrollmentResponse>> getEnrollmentsByEmployeeId(
            @RequestParam Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !employeeId.equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own enrollments");
        }
        return ResponseEntity.ok(enrollmentService.findByEmployeeId(employeeId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(@PathVariable Long id) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !enrollment.getEmployeeId().equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own enrollments");
        }
        return ResponseEntity.ok(enrollment);
    }

    @PostMapping
    @PreAuthorize("hasRole('employee')")
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentRequest request) {
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (jwt != null && !request.getEmployeeId().equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only enroll themselves");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
                enrollmentService.enroll(request.getEmployeeId(), request.getCourseId()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<EnrollmentResponse> approveEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.approve(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.complete(id));
    }
}

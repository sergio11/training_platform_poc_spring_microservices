package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !employeeId.equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own enrollments");
        }
        return ResponseEntity.ok(enrollmentService.findByEmployeeId(employeeId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(@PathVariable Long id) {
        EnrollmentResponse enrollment = enrollmentService.findById(id);
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !enrollment.getEmployeeId().equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own enrollments");
        }
        return ResponseEntity.ok(enrollment);
    }

    @PostMapping
    @PreAuthorize("hasRole('employee')")
    public ResponseEntity<EnrollmentResponse> enroll(
            @RequestParam Long employeeId,
            @RequestParam Long courseId) {
        Jwt jwt = getJwtFromContext();
        if (jwt != null && !employeeId.equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only enroll themselves");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(employeeId, courseId));
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

    private Jwt getJwtFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            return (Jwt) auth.getPrincipal();
        }
        return null;
    }

    private boolean isEmployeeOnly(Jwt jwt) {
        if (jwt == null) return false;
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) return false;
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
        return roles.contains("employee") && !roles.contains("admin") && !roles.contains("instructor");
    }

    private Long extractEmployeeId(Jwt jwt) {
        if (jwt == null) return null;
        java.util.List<String> employeeIds = jwt.getClaimAsStringList("employeeId");
        if (employeeIds != null && !employeeIds.isEmpty()) {
            try {
                return Long.parseLong(employeeIds.get(0));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

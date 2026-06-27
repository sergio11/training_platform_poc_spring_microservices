package com.poc.training.employee.controller;

import com.poc.training.employee.dto.EmployeeRequest;
import com.poc.training.employee.dto.EmployeeResponse;
import com.poc.training.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'instructor')")
    public ResponseEntity<Page<EmployeeResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        return ResponseEntity.ok(employeeService.findAll(PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @GetMapping("/number/{employeeNumber}")
    @PreAuthorize("hasAnyRole('admin', 'instructor')")
    public ResponseEntity<EmployeeResponse> getByEmployeeNumber(@PathVariable String employeeNumber) {
        return ResponseEntity.ok(employeeService.findByEmployeeNumber(employeeNumber));
    }

    @GetMapping("/{id}/exists")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.existsById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

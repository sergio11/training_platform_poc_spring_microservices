package com.poc.training.catalog.controller;

import com.poc.training.catalog.dto.CourseRequest;
import com.poc.training.catalog.dto.CourseResponse;
import com.poc.training.catalog.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<CourseResponse>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        return ResponseEntity.ok(courseService.findAll(PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> deactivateCourse(@PathVariable Long id) {
        courseService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<Void> incrementEnrolledCount(@PathVariable Long id) {
        courseService.incrementEnrolledCount(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<CourseResponse> markCourseAsCompleted(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.markAsCompleted(id));
    }
}

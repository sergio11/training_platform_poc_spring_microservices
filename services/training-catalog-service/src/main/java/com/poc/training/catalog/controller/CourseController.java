package com.poc.training.catalog.controller;

import com.poc.training.catalog.dto.CourseRequest;
import com.poc.training.catalog.dto.CourseResponse;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.shared.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "title", "description", "instructor", "capacity",
        "enrolledCount", "status", "createdAt", "updatedAt"
    );

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<CourseResponse>> getAllCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        validateSortBy(sortBy);
        return ResponseEntity.ok(courseService.search(title, instructor, status, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<CourseResponse>> getActiveCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        validateSortBy(sortBy);
        return ResponseEntity.ok(courseService.findActiveCourses(PageRequest.of(page, size, Sort.by(sortBy))));
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

    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException("Invalid sort field: '" + sortBy + "'. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<CourseResponse> markCourseAsCompleted(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.markAsCompleted(id));
    }

    @PatchMapping("/{id}/seats/increment")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<Void> incrementEnrolledCount(@PathVariable Long id) {
        courseService.incrementEnrolledCount(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/seats/decrement")
    @PreAuthorize("hasAnyRole('instructor', 'admin')")
    public ResponseEntity<Void> decrementEnrolledCount(@PathVariable Long id) {
        courseService.decrementEnrolledCount(id);
        return ResponseEntity.ok().build();
    }
}

package com.poc.training.catalog.service;

import com.poc.training.catalog.dto.CourseRequest;
import com.poc.training.catalog.dto.CourseResponse;
import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import com.poc.training.catalog.specification.CourseSpecification;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    public static CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructor(course.getInstructor())
                .capacity(course.getCapacity())
                .enrolledCount(course.getEnrolledCount())
                .status(course.getStatus().name())
                .build();
    }

    private static Course toEntity(CourseRequest request) {
        return Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .instructor(request.getInstructor())
                .capacity(request.getCapacity())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findAll(Pageable pageable) {
        return courseRepository.findAll(pageable).map(CourseService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> findActiveCourses(Pageable pageable) {
        return courseRepository.findByStatus(CourseStatus.ACTIVE, pageable).map(CourseService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> search(String title, String instructor, CourseStatus status, Pageable pageable) {
        return courseRepository.findAll(CourseSpecification.withFilters(title, instructor, status), pageable)
                .map(CourseService::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return toResponse(course);
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Course course = toEntity(request);
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        if (request.getCapacity() < course.getEnrolledCount()) {
            throw new BusinessException(
                "Cannot reduce capacity to " + request.getCapacity()
                + ". Current enrolled count: " + course.getEnrolledCount());
        }
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setInstructor(request.getInstructor());
        course.setCapacity(request.getCapacity());
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public void deactivate(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        course.setStatus(CourseStatus.INACTIVE);
        courseRepository.save(course);
    }

    @Transactional
    public CourseResponse markAsCompleted(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new BusinessException(
                "Cannot complete course in status: " + course.getStatus() + ". Expected: ACTIVE"
            );
        }

        course.setStatus(CourseStatus.COMPLETED);
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public void incrementEnrolledCount(Long courseId) {
        int updated = courseRepository.incrementEnrolledCountIfAvailable(courseId);
        if (updated == 0) {
            throw new BusinessException("No available seats for course: " + courseId);
        }
    }

    @Transactional
    public void decrementEnrolledCount(Long courseId) {
        int updated = courseRepository.decrementEnrolledCount(courseId);
        if (updated == 0) {
            throw new BusinessException("Cannot decrement enrolled count for course: " + courseId + ". Count is already 0.");
        }
    }
}

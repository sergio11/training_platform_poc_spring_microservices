package com.poc.training.catalog.service;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public List<Course> findActiveCourses() {
        return courseRepository.findByStatus(CourseStatus.ACTIVE);
    }

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    public Course create(Course course) {
        return courseRepository.save(course);
    }

    public Course update(Long id, Course courseDetails) {
        Course course = findById(id);
        course.setTitle(courseDetails.getTitle());
        course.setDescription(courseDetails.getDescription());
        course.setInstructor(courseDetails.getInstructor());
        course.setCapacity(courseDetails.getCapacity());
        return courseRepository.save(course);
    }

    public void deactivate(Long id) {
        Course course = findById(id);
        course.setStatus(CourseStatus.INACTIVE);
        courseRepository.save(course);
    }

    public void incrementEnrolledCount(Long courseId) {
        Course course = findById(courseId);
        if (course.getEnrolledCount() >= course.getCapacity()) {
            throw new RuntimeException("No available seats for course: " + courseId);
        }
        course.setEnrolledCount(course.getEnrolledCount() + 1);
        courseRepository.save(course);
    }
}

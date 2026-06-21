package com.poc.training.catalog.service;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void shouldReturnAllCourses() {
        when(courseRepository.findAll()).thenReturn(List.of(
                Course.builder().id(1L).title("Java Basics").build(),
                Course.builder().id(2L).title("Spring Boot").build()
        ));
        List<Course> courses = courseService.findAll();
        assertEquals(2, courses.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnActiveCourses() {
        when(courseRepository.findByStatus(CourseStatus.ACTIVE)).thenReturn(List.of(
                Course.builder().id(1L).title("Java Basics").status(CourseStatus.ACTIVE).build()
        ));
        List<Course> courses = courseService.findActiveCourses();
        assertEquals(1, courses.size());
    }

    @Test
    void shouldReturnCourseById() {
        Course course = Course.builder().id(1L).title("Java Basics").instructor("John").capacity(30).enrolledCount(0).status(CourseStatus.ACTIVE).build();
        when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(course));
        Course found = courseService.findById(1L);
        assertNotNull(found);
        assertEquals("Java Basics", found.getTitle());
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> courseService.findById(99L));
    }

    @Test
    void shouldCreateCourse() {
        Course course = Course.builder().title("Java Basics").instructor("John").capacity(30).build();
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        Course created = courseService.create(course);
        assertNotNull(created);
        assertEquals("Java Basics", created.getTitle());
    }

    @Test
    void shouldUpdateCourse() {
        Course existing = Course.builder().id(1L).title("Old Title").instructor("Old").capacity(20).enrolledCount(0).status(CourseStatus.ACTIVE).build();
        Course updated = Course.builder().title("New Title").instructor("New").capacity(40).build();
        when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        Course result = courseService.update(1L, updated);
        assertEquals("New Title", result.getTitle());
        assertEquals("New", result.getInstructor());
        assertEquals(40, result.getCapacity());
    }

    @Test
    void shouldDeactivateCourse() {
        Course course = Course.builder().id(1L).title("Java Basics").status(CourseStatus.ACTIVE).build();
        when(courseRepository.findById(1L)).thenReturn(java.util.Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        courseService.deactivate(1L);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void shouldIncrementEnrolledCount() {
        when(courseRepository.incrementEnrolledCountIfAvailable(1L)).thenReturn(1);
        courseService.incrementEnrolledCount(1L);
        verify(courseRepository, times(1)).incrementEnrolledCountIfAvailable(1L);
    }

    @Test
    void shouldThrowExceptionWhenNoSeatsAvailable() {
        when(courseRepository.incrementEnrolledCountIfAvailable(1L)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> courseService.incrementEnrolledCount(1L));
    }
}

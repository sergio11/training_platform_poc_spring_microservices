package com.poc.training.catalog.service;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void shouldReturnAllCourses() {
        Course course1 = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        Course course2 = Course.builder()
                .id(2L)
                .title("Spring Boot")
                .instructor("Jane")
                .capacity(25)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseRepository.findAll()).thenReturn(Arrays.asList(course1, course2));

        List<Course> courses = courseService.findAll();

        assertEquals(2, courses.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnCourseById() {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course found = courseService.findById(1L);

        assertNotNull(found);
        assertEquals("Java Basics", found.getTitle());
    }

    @Test
    void shouldCreateCourse() {
        Course course = Course.builder()
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .build();

        when(courseRepository.save(any(Course.class))).thenReturn(course);

        Course created = courseService.create(course);

        assertNotNull(created);
        assertEquals("Java Basics", created.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void shouldUpdateCourse() {
        Course existing = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        Course updated = Course.builder()
                .title("Java Advanced")
                .instructor("John Updated")
                .capacity(40)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenReturn(updated);

        Course result = courseService.update(1L, updated);

        assertNotNull(result);
        assertEquals("Java Advanced", result.getTitle());
    }

    @Test
    void shouldDeactivateCourse() {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        courseService.deactivate(1L);

        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void shouldIncrementEnrolledCount() {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        courseService.incrementEnrolledCount(1L);

        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void shouldThrowExceptionWhenNoSeatsAvailable() {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .capacity(30)
                .enrolledCount(30)
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(RuntimeException.class, () -> courseService.incrementEnrolledCount(1L));
    }
}

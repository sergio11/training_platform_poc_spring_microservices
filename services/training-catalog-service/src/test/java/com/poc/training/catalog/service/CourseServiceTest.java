package com.poc.training.catalog.service;

import com.poc.training.catalog.dto.CourseRequest;
import com.poc.training.catalog.dto.CourseResponse;
import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

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
        when(courseRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(
                Course.builder().id(1L).title("Java Basics").status(CourseStatus.ACTIVE).build(),
                Course.builder().id(2L).title("Spring Boot").status(CourseStatus.ACTIVE).build()
        )));
        var courses = courseService.findAll(PageRequest.of(0, 10));
        assertEquals(2, courses.getContent().size());
        verify(courseRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void shouldReturnActiveCourses() {
        when(courseRepository.findByStatus(eq(CourseStatus.ACTIVE), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(
                Course.builder().id(1L).title("Java Basics").status(CourseStatus.ACTIVE).build()
        )));
        var courses = courseService.findActiveCourses(PageRequest.of(0, 10));
        assertEquals(1, courses.getContent().size());
    }

    @Test
    void shouldReturnCourseById() {
        Course course = Course.builder().id(1L).title("Java Basics").instructor("John").capacity(30).enrolledCount(0).status(CourseStatus.ACTIVE).build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        CourseResponse result = courseService.findById(1L);
        assertNotNull(result);
        assertEquals("Java Basics", result.getTitle());
    }

    @Test
    void shouldThrowExceptionWhenCourseNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> courseService.findById(99L));
    }

    @Test
    void shouldCreateCourse() {
        CourseRequest request = new CourseRequest();
        request.setTitle("Java Basics");
        request.setInstructor("John");
        request.setCapacity(30);

        when(courseRepository.save(any(Course.class))).thenReturn(
                Course.builder().id(1L).title("Java Basics").instructor("John").capacity(30).status(CourseStatus.ACTIVE).build()
        );
        CourseResponse created = courseService.create(request);
        assertNotNull(created);
        assertEquals("Java Basics", created.getTitle());
    }

    @Test
    void shouldUpdateCourse() {
        Course existing = Course.builder().id(1L).title("Old Title").instructor("Old").capacity(20).enrolledCount(0).status(CourseStatus.ACTIVE).build();
        CourseRequest request = new CourseRequest();
        request.setTitle("New Title");
        request.setInstructor("New");
        request.setCapacity(40);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        CourseResponse result = courseService.update(1L, request);
        assertEquals("New Title", result.getTitle());
        assertEquals("New", result.getInstructor());
        assertEquals(40, result.getCapacity());
    }

    @Test
    void shouldDeactivateCourse() {
        Course course = Course.builder().id(1L).title("Java Basics").status(CourseStatus.ACTIVE).build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
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
        assertThrows(BusinessException.class, () -> courseService.incrementEnrolledCount(1L));
    }
}

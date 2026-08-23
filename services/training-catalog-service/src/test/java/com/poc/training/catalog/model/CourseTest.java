package com.poc.training.catalog.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CourseTest {

    @Test
    void shouldSetDefaultValuesOnPrePersist() {
        Course course = Course.builder()
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .build();

        course.onCreate();

        assertEquals(CourseStatus.ACTIVE, course.getStatus());
        assertEquals(0, course.getEnrolledCount());
        assertNotNull(course.getCreatedAt());
        assertNotNull(course.getUpdatedAt());
    }

    @Test
    void shouldUpdateTimestampOnPreUpdate() {
        Course course = Course.builder()
                .title("Java Basics")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        LocalDateTime beforeUpdate = LocalDateTime.now();
        course.onUpdate();

        assertTrue(course.getUpdatedAt().isAfter(beforeUpdate) || course.getUpdatedAt().isEqual(beforeUpdate));
    }

    @Test
    void shouldCreateCourseWithBuilder() {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .description("Introduction to Java")
                .instructor("John Doe")
                .capacity(30)
                .enrolledCount(5)
                .status(CourseStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertEquals(1L, course.getId());
        assertEquals("Java Basics", course.getTitle());
        assertEquals("Introduction to Java", course.getDescription());
        assertEquals("John Doe", course.getInstructor());
        assertEquals(30, course.getCapacity());
        assertEquals(5, course.getEnrolledCount());
        assertEquals(CourseStatus.ACTIVE, course.getStatus());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        Course course = new Course();
        assertNotNull(course);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        Course course = new Course(1L, "Title", "Desc", "Instructor", 30, 0, CourseStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        assertEquals("Title", course.getTitle());
    }

    @Test
    void shouldNotOverrideDefaultsOnPrePersistWhenAlreadySet() {
        Course course = Course.builder()
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(5)
                .status(CourseStatus.INACTIVE)
                .build();

        course.onCreate();

        assertEquals(CourseStatus.INACTIVE, course.getStatus());
        assertEquals(5, course.getEnrolledCount());
        assertNotNull(course.getCreatedAt());
        assertNotNull(course.getUpdatedAt());
    }
}

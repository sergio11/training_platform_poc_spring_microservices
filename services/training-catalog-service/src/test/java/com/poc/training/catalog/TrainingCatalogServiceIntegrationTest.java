package com.poc.training.catalog;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class TrainingCatalogServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldSaveAndRetrieveCourse() {
        Course course = Course.builder()
                .title("Java Basics")
                .description("Introduction to Java")
                .instructor("John Doe")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        Course saved = courseRepository.save(course);

        assertNotNull(saved.getId());
        assertEquals("Java Basics", saved.getTitle());

        Course found = courseRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Java Basics", found.getTitle());
        assertEquals(CourseStatus.ACTIVE, found.getStatus());
    }

    @Test
    void shouldFindActiveCourses() {
        Course active = Course.builder()
                .title("Active Course")
                .instructor("John")
                .capacity(30)
                .status(CourseStatus.ACTIVE)
                .build();

        Course inactive = Course.builder()
                .title("Inactive Course")
                .instructor("Jane")
                .capacity(25)
                .status(CourseStatus.INACTIVE)
                .build();

        courseRepository.save(active);
        courseRepository.save(inactive);

        var activeCourses = courseRepository.findByStatus(CourseStatus.ACTIVE, PageRequest.of(0, 10));
        assertEquals(1, activeCourses.getContent().size());
        assertEquals("Active Course", activeCourses.getContent().get(0).getTitle());
    }
}

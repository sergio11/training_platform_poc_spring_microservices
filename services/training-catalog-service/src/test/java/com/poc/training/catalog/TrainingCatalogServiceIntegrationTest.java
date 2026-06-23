package com.poc.training.catalog;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TrainingCatalogServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("training_catalog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CourseRepository courseRepository;

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

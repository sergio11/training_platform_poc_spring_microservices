package com.poc.training.catalog.integration;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.shared.exception.ResourceNotFoundException;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class CourseServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
    }

    @Test
    void shouldCreateCourse() {
        var request = new com.poc.training.catalog.dto.CourseRequest();
        request.setTitle("Java Advanced");
        request.setDescription("Advanced Java topics");
        request.setInstructor("John Doe");
        request.setCapacity(25);
        var response = courseService.create(request);

        assertNotNull(response.getId());
        assertEquals("Java Advanced", response.getTitle());
        assertEquals(CourseStatus.ACTIVE.name(), response.getStatus());
    }

    @Test
    void shouldFindCourseById() {
        Course saved = courseRepository.save(Course.builder()
                .title("Spring Boot").instructor("Jane").capacity(30).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        var response = courseService.findById(saved.getId());

        assertEquals("Spring Boot", response.getTitle());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> courseService.findById(99999L));
    }

    @Test
    void shouldSearchCoursesByTitle() {
        courseRepository.save(Course.builder().title("Kubernetes").instructor("A").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());
        courseRepository.save(Course.builder().title("Docker").instructor("B").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        var result = courseService.search("Kubernetes", null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals("Kubernetes", result.getContent().get(0).getTitle());
    }

    @Test
    void shouldSearchCoursesByStatus() {
        courseRepository.save(Course.builder().title("Active1").instructor("A").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());
        courseRepository.save(Course.builder().title("Inactive1").instructor("B").capacity(10).enrolledCount(0).status(CourseStatus.INACTIVE).build());

        var result = courseService.search(null, null, CourseStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldIncrementEnrolledCount() {
        Course saved = courseRepository.save(Course.builder()
                .title("Capacity").instructor("X").capacity(5).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        courseService.incrementEnrolledCount(saved.getId());

        Course updated = courseRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, updated.getEnrolledCount());
    }

    @Test
    void shouldThrowWhenNoSeatsAvailable() {
        Course saved = courseRepository.save(Course.builder()
                .title("Full").instructor("X").capacity(1).enrolledCount(1).status(CourseStatus.ACTIVE).build());

        assertThrows(com.poc.training.shared.exception.BusinessException.class,
                () -> courseService.incrementEnrolledCount(saved.getId()));
    }

    @Test
    void shouldDecrementEnrolledCount() {
        Course saved = courseRepository.save(Course.builder()
                .title("Decrement").instructor("X").capacity(5).enrolledCount(2).status(CourseStatus.ACTIVE).build());

        courseService.decrementEnrolledCount(saved.getId());

        Course updated = courseRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, updated.getEnrolledCount());
    }

    @Test
    void shouldDeactivateCourse() {
        Course saved = courseRepository.save(Course.builder()
                .title("To Deactivate").instructor("X").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        courseService.deactivate(saved.getId());

        Course updated = courseRepository.findById(saved.getId()).orElseThrow();
        assertEquals(CourseStatus.INACTIVE, updated.getStatus());
    }

    @Test
    void shouldMarkCourseAsCompleted() {
        Course saved = courseRepository.save(Course.builder()
                .title("To Complete").instructor("X").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        var response = courseService.markAsCompleted(saved.getId());

        assertEquals(CourseStatus.COMPLETED.name(), response.getStatus());
    }

    @Test
    void shouldThrowWhenMarkingNonActiveCourseAsCompleted() {
        Course saved = courseRepository.save(Course.builder()
                .title("Inactive").instructor("X").capacity(10).enrolledCount(0).status(CourseStatus.INACTIVE).build());

        assertThrows(com.poc.training.shared.exception.BusinessException.class,
                () -> courseService.markAsCompleted(saved.getId()));
    }
}

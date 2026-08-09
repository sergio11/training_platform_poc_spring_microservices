package com.poc.training.catalog.specification;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<Course> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    private void setupMocks() {
        Path statusPath = mock(Path.class);
        Path titlePath = mock(Path.class);
        Path instructorPath = mock(Path.class);
        lenient().when(root.get("status")).thenReturn(statusPath);
        lenient().when(root.get("title")).thenReturn(titlePath);
        lenient().when(root.get("instructor")).thenReturn(instructorPath);
        lenient().when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        lenient().when(cb.and(any(Predicate.class))).thenAnswer(inv -> mock(Predicate.class));
        lenient().when(cb.and(any(Predicate[].class))).thenAnswer(inv -> mock(Predicate.class));
        lenient().when(cb.and()).thenAnswer(inv -> mock(Predicate.class));
    }

    @Test
    void shouldReturnNonNullSpecification() {
        assertNotNull(CourseSpecification.withFilters("Java", "John", CourseStatus.ACTIVE));
    }

    @Test
    void shouldBuildPredicateWithAllFilters() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters("Java", "John", CourseStatus.ACTIVE);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(cb).equal(any(), eq(CourseStatus.ACTIVE));
        verify(cb).like(any(), eq("%java%"));
        verify(cb).like(any(), eq("%john%"));
    }

    @Test
    void shouldBuildPredicateWithStatusOnly() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters(null, null, CourseStatus.ACTIVE);
        spec.toPredicate(root, query, cb);
        verify(cb).equal(any(), eq(CourseStatus.ACTIVE));
    }

    @Test
    void shouldBuildPredicateWithTitleOnly() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters("Java", null, null);
        spec.toPredicate(root, query, cb);
        verify(cb).like(any(), eq("%java%"));
    }

    @Test
    void shouldBuildPredicateWithInstructorOnly() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters(null, "John", null);
        spec.toPredicate(root, query, cb);
        verify(cb).like(any(), eq("%john%"));
    }

    @Test
    void shouldBuildPredicateWithAllNulls() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters(null, null, null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
        verify(root, never()).get("status");
        verify(root, never()).get("title");
        verify(root, never()).get("instructor");
    }

    @Test
    void shouldSkipBlankTitle() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters("  ", null, null);
        spec.toPredicate(root, query, cb);
        verify(root, never()).get("title");
    }

    @Test
    void shouldSkipBlankInstructor() {
        setupMocks();
        Specification<Course> spec = CourseSpecification.withFilters(null, "  ", null);
        spec.toPredicate(root, query, cb);
        verify(root, never()).get("instructor");
    }

    @Test
    void shouldUsePrivateConstructor() throws Exception {
        var constructor = CourseSpecification.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        CourseSpecification instance = constructor.newInstance();
        assertNotNull(instance);
    }
}

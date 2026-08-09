package com.poc.training.catalog.specification;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import org.springframework.data.jpa.domain.Specification;

public class CourseSpecification {

    private CourseSpecification() {
    }

    public static Specification<Course> withFilters(String title, String instructor, CourseStatus status) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (instructor != null && !instructor.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("instructor")), "%" + instructor.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

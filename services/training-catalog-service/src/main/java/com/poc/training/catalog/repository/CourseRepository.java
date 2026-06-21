package com.poc.training.catalog.repository;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Course c SET c.enrolledCount = c.enrolledCount + 1 WHERE c.id = :courseId AND c.enrolledCount < c.capacity")
    int incrementEnrolledCountIfAvailable(@Param("courseId") Long courseId);
}

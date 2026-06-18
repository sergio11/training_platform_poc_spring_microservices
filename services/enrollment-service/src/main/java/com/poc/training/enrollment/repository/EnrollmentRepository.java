package com.poc.training.enrollment.repository;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByEmployeeId(Long employeeId);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    boolean existsByEmployeeIdAndCourseIdAndStatusIn(Long employeeId, Long courseId, List<EnrollmentStatus> statuses);
}

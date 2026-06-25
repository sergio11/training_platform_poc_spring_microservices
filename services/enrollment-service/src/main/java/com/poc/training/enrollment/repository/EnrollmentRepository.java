package com.poc.training.enrollment.repository;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Page<Enrollment> findByEmployeeId(Long employeeId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.employeeId = :employeeId AND e.courseId = :courseId AND e.status IN :statuses")
    List<Enrollment> findByEmployeeIdAndCourseIdAndStatusInWithLock(
            @Param("employeeId") Long employeeId,
            @Param("courseId") Long courseId,
            @Param("statuses") List<EnrollmentStatus> statuses);
}

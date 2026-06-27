package com.poc.training.employee.repository;

import com.poc.training.employee.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeNumber(String employeeNumber);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByKeycloakUserId(String keycloakUserId);
    Page<Employee> findByActiveTrue(Pageable pageable);
}

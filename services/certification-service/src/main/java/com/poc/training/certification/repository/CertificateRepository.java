package com.poc.training.certification.repository;

import com.poc.training.certification.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByEmployeeId(Long employeeId);
    Optional<Certificate> findByEnrollmentId(Long enrollmentId);
    Optional<Certificate> findByCertificateNumber(String certificateNumber);
}

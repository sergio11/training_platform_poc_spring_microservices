package com.poc.training.certification.repository;

import com.poc.training.certification.model.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Page<Certificate> findByEmployeeId(Long employeeId, Pageable pageable);
    Optional<Certificate> findByEnrollmentId(Long enrollmentId);
    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    @Query(value = "SELECT nextval('certificate_number_seq')", nativeQuery = true)
    Long nextCertificateNumber();
}

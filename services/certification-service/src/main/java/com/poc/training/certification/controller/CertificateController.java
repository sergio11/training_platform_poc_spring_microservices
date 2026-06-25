package com.poc.training.certification.controller;

import com.poc.training.certification.dto.CertificateResponse;
import com.poc.training.certification.service.CertificationService;
import com.poc.training.shared.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificationService certificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<CertificateResponse>> getCertificatesByEmployeeId(
            @RequestParam Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !employeeId.equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own certificates");
        }
        return ResponseEntity.ok(certificationService.findByEmployeeId(employeeId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<CertificateResponse> getCertificateById(@PathVariable Long id) {
        CertificateResponse certificate = certificationService.findById(id);
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !certificate.getEmployeeId().equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own certificates");
        }
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/number/{number}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<CertificateResponse> getCertificateByNumber(@PathVariable String number) {
        CertificateResponse certificate = certificationService.findByCertificateNumber(number);
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !certificate.getEmployeeId().equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own certificates");
        }
        return ResponseEntity.ok(certificate);
    }
}

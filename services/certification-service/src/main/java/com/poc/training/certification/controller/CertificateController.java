package com.poc.training.certification.controller;

import com.poc.training.certification.dto.CertificateResponse;
import com.poc.training.certification.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !employeeId.equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own certificates");
        }
        return ResponseEntity.ok(certificationService.findByEmployeeId(employeeId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<CertificateResponse> getCertificateById(@PathVariable Long id) {
        CertificateResponse certificate = certificationService.findById(id);
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !certificate.getEmployeeId().equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own certificates");
        }
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/number/{number}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<CertificateResponse> getCertificateByNumber(@PathVariable String number) {
        CertificateResponse certificate = certificationService.findByCertificateNumber(number);
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !certificate.getEmployeeId().equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own certificates");
        }
        return ResponseEntity.ok(certificate);
    }

    private Jwt getJwtFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            return (Jwt) auth.getPrincipal();
        }
        return null;
    }

    private boolean isEmployeeOnly(Jwt jwt) {
        if (jwt == null) return false;
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) return false;
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
        return roles.contains("employee") && !roles.contains("admin") && !roles.contains("instructor");
    }

    private Long extractEmployeeId(Jwt jwt) {
        if (jwt == null) return null;
        java.util.List<String> employeeIds = jwt.getClaimAsStringList("employeeId");
        if (employeeIds != null && !employeeIds.isEmpty()) {
            try {
                return Long.parseLong(employeeIds.get(0));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

package com.poc.training.certification.controller;

import com.poc.training.certification.model.Certificate;
import com.poc.training.certification.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificationService certificationService;

    @GetMapping
    public ResponseEntity<List<Certificate>> getCertificatesByEmployeeId(@RequestParam Long employeeId) {
        return ResponseEntity.ok(certificationService.findByEmployeeId(employeeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificateById(@PathVariable Long id) {
        return ResponseEntity.ok(certificationService.findById(id));
    }

    @GetMapping("/number/{number}")
    public ResponseEntity<Certificate> getCertificateByNumber(@PathVariable String number) {
        return ResponseEntity.ok(certificationService.findByCertificateNumber(number));
    }
}

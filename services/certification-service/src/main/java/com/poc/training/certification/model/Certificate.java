package com.poc.training.certification.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String certificateNumber;

    @Column(nullable = false, unique = true)
    private Long enrollmentId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
    }
}

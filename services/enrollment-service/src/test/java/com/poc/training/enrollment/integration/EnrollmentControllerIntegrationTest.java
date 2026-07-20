package com.poc.training.enrollment.integration;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.model.OutboxEvent;
import com.poc.training.enrollment.repository.EnrollmentRepository;
import com.poc.training.enrollment.repository.OutboxEventRepository;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class EnrollmentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        enrollmentRepository.deleteAll();
    }

    @Test
    void shouldGetEnrollmentsByEmployeeId() throws Exception {
        enrollmentRepository.save(Enrollment.builder()
                .employeeId(100L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        mockMvc.perform(get("/api/v1/enrollments")
                        .param("employeeId", "100")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldGetEnrollmentById() throws Exception {
        Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                .employeeId(100L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        mockMvc.perform(get("/api/v1/enrollments/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturn404ForNonExistentEnrollment() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/{id}", 99999)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .param("employeeId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400ForInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .param("employeeId", "1")
                        .param("sortBy", "invalidField")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeeCannotViewOtherEnrollments() throws Exception {
        enrollmentRepository.save(Enrollment.builder()
                .employeeId(200L).courseId(1L).status(EnrollmentStatus.PENDING).build());

        mockMvc.perform(get("/api/v1/enrollments")
                        .param("employeeId", "200")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }
}

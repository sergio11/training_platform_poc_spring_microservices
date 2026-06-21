package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.model.Enrollment;
import com.poc.training.enrollment.model.EnrollmentStatus;
import com.poc.training.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void shouldReturnEnrollmentsByEmployeeId() throws Exception {
        Enrollment e = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        when(enrollmentService.findByEmployeeId(1L)).thenReturn(Arrays.asList(e));

        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(1));
    }

    @Test
    void shouldReturnEnrollmentById() throws Exception {
        Enrollment e = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        when(enrollmentService.findById(1L)).thenReturn(e);

        mockMvc.perform(get("/api/v1/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldCreateEnrollment() throws Exception {
        Enrollment e = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.PENDING)
                .build();

        when(enrollmentService.enroll(eq(1L), eq(1L))).thenReturn(e);

        mockMvc.perform(post("/api/v1/enrollments").param("employeeId", "1").param("courseId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    void shouldApproveEnrollment() throws Exception {
        Enrollment e = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.APPROVED)
                .build();

        when(enrollmentService.approve(1L)).thenReturn(e);

        mockMvc.perform(patch("/api/v1/enrollments/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldCompleteEnrollment() throws Exception {
        Enrollment e = Enrollment.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status(EnrollmentStatus.COMPLETED)
                .build();

        when(enrollmentService.complete(1L)).thenReturn(e);

        mockMvc.perform(patch("/api/v1/enrollments/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}

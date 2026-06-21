package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
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
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("PENDING")
                .build();

        when(enrollmentService.findByEmployeeId(eq(1L), any())).thenReturn(new PageImpl<>(Arrays.asList(response)));

        mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId").value(1));
    }

    @Test
    void shouldReturnEnrollmentById() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("PENDING")
                .build();

        when(enrollmentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldCreateEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("PENDING")
                .build();

        when(enrollmentService.enroll(eq(1L), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/enrollments").param("employeeId", "1").param("courseId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    void shouldApproveEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("APPROVED")
                .build();

        when(enrollmentService.approve(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/enrollments/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldCompleteEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("COMPLETED")
                .build();

        when(enrollmentService.complete(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/enrollments/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}

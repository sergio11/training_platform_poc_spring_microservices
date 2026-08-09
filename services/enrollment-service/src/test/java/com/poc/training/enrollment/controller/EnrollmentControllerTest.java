package com.poc.training.enrollment.controller;

import com.poc.training.enrollment.dto.EnrollmentRequest;
import com.poc.training.enrollment.dto.EnrollmentResponse;
import com.poc.training.enrollment.service.EnrollmentService;
import com.poc.training.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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

    @MockitoBean
    private JwtDecoder jwtDecoder;

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
    void shouldReturnPaginatedEnrollments() throws Exception {
        EnrollmentResponse r1 = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").build();
        EnrollmentResponse r2 = EnrollmentResponse.builder()
                .id(2L).employeeId(1L).courseId(2L).status("APPROVED").build();

        org.springframework.data.domain.Page<EnrollmentResponse> page =
                new PageImpl<>(Arrays.asList(r1, r2),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2);

        when(enrollmentService.findByEmployeeId(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/enrollments")
                        .param("employeeId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldReturnEmptyPageWhenNoEnrollments() throws Exception {
        org.springframework.data.domain.Page<EnrollmentResponse> emptyPage =
                new PageImpl<>(Arrays.asList(),
                        org.springframework.data.domain.PageRequest.of(0, 20), 0);

        when(enrollmentService.findByEmployeeId(eq(999L), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/enrollments")
                        .param("employeeId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
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

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":1,\"courseId\":1}"))
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

    @Test
    void shouldRejectEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("REJECTED")
                .build();

        when(enrollmentService.reject(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/enrollments/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldCancelEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L)
                .employeeId(1L)
                .courseId(1L)
                .status("CANCELLED")
                .build();

        when(enrollmentService.findById(1L)).thenReturn(response);
        when(enrollmentService.cancel(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/enrollments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDenyEmployeeViewingOtherEnrollments() throws Exception {
        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "2"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldDenyEmployeeViewingOtherEnrollmentById() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).status("PENDING").build();
        when(enrollmentService.findById(1L)).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/enrollments/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldDenyEmployeeEnrollingForOther() throws Exception {
        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"employeeId\":2,\"courseId\":1}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldDenyEmployeeCancellingOtherEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(2L).courseId(1L).status("APPROVED").build();
        when(enrollmentService.findById(1L)).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(patch("/api/v1/enrollments/1/cancel"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldAllowEmployeeViewingOwnEnrollmentsList() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").build();
        when(enrollmentService.findByEmployeeId(eq(1L), any())).thenReturn(new PageImpl<>(Arrays.asList(response)));

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/enrollments").param("employeeId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employeeId").value(1));
        }
    }

    @Test
    void shouldAllowEmployeeViewingOwnEnrollmentById() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").build();
        when(enrollmentService.findById(1L)).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(get("/api/v1/enrollments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId").value(1));
        }
    }

    @Test
    void shouldAllowEmployeeEnrollingSelf() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("PENDING").build();
        when(enrollmentService.enroll(eq(1L), eq(1L))).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"employeeId\":1,\"courseId\":1}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeId").value(1));
        }
    }

    @Test
    void shouldAllowEmployeeCancellingOwnEnrollment() throws Exception {
        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(1L).employeeId(1L).courseId(1L).status("CANCELLED").build();
        when(enrollmentService.findById(1L)).thenReturn(response);
        when(enrollmentService.cancel(1L)).thenReturn(response);

        Jwt mockJwt = mock(Jwt.class);
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            jwtUtils.when(JwtUtils::getJwtFromContext).thenReturn(mockJwt);
            jwtUtils.when(() -> JwtUtils.isEmployeeOnly(mockJwt)).thenReturn(true);
            jwtUtils.when(() -> JwtUtils.extractEmployeeIdAsLong(mockJwt)).thenReturn(1L);

            mockMvc.perform(patch("/api/v1/enrollments/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId").value(1));
        }
    }
}

package com.poc.training.employee.controller;

import com.poc.training.employee.dto.EmployeeRequest;
import com.poc.training.employee.dto.EmployeeResponse;
import com.poc.training.employee.service.EmployeeService;
import com.poc.training.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private EmployeeResponse buildResponse(Long id, String empNumber, String name) {
        return EmployeeResponse.builder()
                .id(id).employeeNumber(empNumber).fullName(name)
                .email(name.toLowerCase() + "@test.com").department("IT").position("Dev")
                .active(true).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void shouldGetAllEmployees() throws Exception {
        EmployeeResponse r1 = buildResponse(1L, "EMP001", "Alice");
        EmployeeResponse r2 = buildResponse(2L, "EMP002", "Bob");
        when(employeeService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(r1, r2), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldGetEmployeeById() throws Exception {
        when(employeeService.findById(1L)).thenReturn(buildResponse(1L, "EMP001", "Alice"));

        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("EMP001"));
    }

    @Test
    void shouldGetEmployeeByNumber() throws Exception {
        when(employeeService.findByEmployeeNumber("EMP001")).thenReturn(buildResponse(1L, "EMP001", "Alice"));

        mockMvc.perform(get("/api/v1/employees/number/EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value("EMP001"));
    }

    @Test
    void shouldCheckExistsById() throws Exception {
        when(employeeService.existsById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/employees/1/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldCreateEmployee() throws Exception {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP003").fullName("Charlie")
                .email("charlie@test.com").department("Eng").position("Dev").build();
        when(employeeService.create(any(EmployeeRequest.class)))
                .thenReturn(buildResponse(3L, "EMP003", "Charlie"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType("application/json")
                        .content("{\"employeeNumber\":\"EMP003\",\"fullName\":\"Charlie\",\"email\":\"charlie@test.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNumber").value("EMP003"));
    }

    @Test
    void shouldUpdateEmployee() throws Exception {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP001").fullName("Alice Updated")
                .email("alice@test.com").department("Sales").position("Manager").build();
        when(employeeService.update(eq(1L), any(EmployeeRequest.class)))
                .thenReturn(buildResponse(1L, "EMP001", "Alice Updated"));

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType("application/json")
                        .content("{\"employeeNumber\":\"EMP001\",\"fullName\":\"Alice Updated\",\"email\":\"alice@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Updated"));
    }

    @Test
    void shouldDeactivateEmployee() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isNoContent());
        verify(employeeService).deactivate(1L);
    }

    @Test
    void shouldReturn400ForInvalidSortBy() throws Exception {
        mockMvc.perform(get("/api/v1/employees").param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCreatingWithBlankFields() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .contentType("application/json")
                        .content("{\"employeeNumber\":\"\",\"fullName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnEmptyPageWhenNoEmployees() throws Exception {
        when(employeeService.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}

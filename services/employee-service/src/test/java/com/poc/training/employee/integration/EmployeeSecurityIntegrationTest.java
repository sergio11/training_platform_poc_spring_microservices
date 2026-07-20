package com.poc.training.employee.integration;

import com.poc.training.employee.model.Employee;
import com.poc.training.employee.repository.EmployeeRepository;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class EmployeeSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void adminCanAccessAllEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType("application/json")
                        .content("{\"employeeNumber\":\"EMP-SEC-001\",\"fullName\":\"Admin Test\",\"email\":\"admin@t.test\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void instructorCanGetEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_instructor"))))
                .andExpect(status().isOk());
    }

    @Test
    void instructorCannotCreateEmployees() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_instructor")))
                        .contentType("application/json")
                        .content("{\"employeeNumber\":\"EMP-SEC-002\",\"fullName\":\"Inst\",\"email\":\"inst@t.test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotAccessAllEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanGetOwnById() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-SEC-003").fullName("Self").email("self@t.test").active(true).build());

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCanCheckExists() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-SEC-004").fullName("Exists").email("exists@t.test").active(true).build());

        mockMvc.perform(get("/api/v1/employees/{id}/exists", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotCreateEmployees() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee")))
                        .contentType("application/json")
                        .content("{\"employeeNumber\":\"EMP-SEC-005\",\"fullName\":\"Emp\",\"email\":\"emp@t.test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }
}

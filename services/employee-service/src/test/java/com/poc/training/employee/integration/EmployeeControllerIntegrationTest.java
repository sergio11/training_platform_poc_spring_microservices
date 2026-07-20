package com.poc.training.employee.integration;

import com.poc.training.employee.model.Employee;
import com.poc.training.employee.repository.EmployeeRepository;
import com.poc.training.testsupport.AbstractIntegrationTest;
import com.poc.training.testsupport.container.KeycloakTestContainer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class EmployeeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void shouldGetAllEmployees() throws Exception {
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-CTL-001").fullName("Alice").email("alice@t.test").active(true).build());

        mockMvc.perform(get("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldGetEmployeeById() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-CTL-002").fullName("Bob").email("bob@t.test").active(true).build());

        mockMvc.perform(get("/api/v1/employees/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Bob"))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-CTL-002"));
    }

    @Test
    void shouldGetEmployeeByEmployeeNumber() throws Exception {
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-NUM-001").fullName("Charlie").email("charlie@t.test").active(true).build());

        mockMvc.perform(get("/api/v1/employees/number/{employeeNumber}", "EMP-NUM-001")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Charlie"));
    }

    @Test
    void shouldCheckExistsById() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-EX-001").fullName("Diana").email("diana@t.test").active(true).build());

        mockMvc.perform(get("/api/v1/employees/{id}/exists", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldCreateEmployee() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeNumber\":\"EMP-CRT-001\",\"fullName\":\"Eve\",\"email\":\"eve@t.test\",\"department\":\"IT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.employeeNumber").value("EMP-CRT-001"));
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeNumber\":\"\",\"fullName\":\"\",\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeactivateEmployee() throws Exception {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-DEL-001").fullName("ToDelete").email("del@t.test").active(true).build());

        mockMvc.perform(delete("/api/v1/employees/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404ForNonExistentEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}", 99999)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenEmployeeRoleTriesToGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }
}

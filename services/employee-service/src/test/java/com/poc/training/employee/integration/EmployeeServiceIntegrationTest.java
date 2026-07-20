package com.poc.training.employee.integration;

import com.poc.training.employee.dto.EmployeeRequest;
import com.poc.training.employee.dto.EmployeeResponse;
import com.poc.training.employee.model.Employee;
import com.poc.training.employee.repository.EmployeeRepository;
import com.poc.training.employee.service.EmployeeService;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    @Order(1)
    void shouldCreateEmployee() {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP-INT-001")
                .fullName("Alice Integration")
                .email("alice@int.test")
                .department("Engineering")
                .position("Developer")
                .build();

        EmployeeResponse response = employeeService.create(request);

        assertNotNull(response.getId());
        assertEquals("EMP-INT-001", response.getEmployeeNumber());
        assertEquals("Alice Integration", response.getFullName());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @Order(2)
    void shouldFindEmployeeById() {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-INT-002")
                .fullName("Bob")
                .email("bob@int.test")
                .active(true)
                .build());

        EmployeeResponse response = employeeService.findById(saved.getId());

        assertEquals("Bob", response.getFullName());
        assertEquals("EMP-INT-002", response.getEmployeeNumber());
    }

    @Test
    @Order(3)
    void shouldFindEmployeeByEmployeeNumber() {
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-FIND-001")
                .fullName("Charlie")
                .email("charlie@int.test")
                .active(true)
                .build());

        EmployeeResponse response = employeeService.findByEmployeeNumber("EMP-FIND-001");

        assertEquals("Charlie", response.getFullName());
    }

    @Test
    @Order(4)
    void shouldCheckExistsById() {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-EXISTS-001")
                .fullName("Diana")
                .email("diana@int.test")
                .active(true)
                .build());

        assertTrue(employeeService.existsById(saved.getId()));
        assertFalse(employeeService.existsById(99999L));
    }

    @Test
    @Order(5)
    void shouldFindAllActiveEmployees() {
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-ACTIVE-1").fullName("E1").email("e1@t.test").active(true).build());
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-ACTIVE-2").fullName("E2").email("e2@t.test").active(true).build());
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-INACTIVE-1").fullName("E3").email("e3@t.test").active(false).build());

        var result = employeeService.findAll(PageRequest.of(0, 10));

        assertEquals(2, result.getContent().size());
    }

    @Test
    @Order(6)
    void shouldThrowWhenEmployeeNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> employeeService.findById(99999L));
    }

    @Test
    @Order(7)
    void shouldThrowWhenDuplicateEmployeeNumber() {
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-DUP-001").fullName("Existing").email("existing@t.test").active(true).build());

        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP-DUP-001")
                .fullName("Duplicate")
                .email("dup@t.test")
                .build();

        assertThrows(BusinessException.class, () -> employeeService.create(request));
    }

    @Test
    @Order(8)
    void shouldThrowWhenDuplicateEmail() {
        employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-DUP-E1").fullName("Existing").email("dup@t.test").active(true).build());

        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP-DUP-E2")
                .fullName("Duplicate")
                .email("dup@t.test")
                .build();

        assertThrows(BusinessException.class, () -> employeeService.create(request));
    }

    @Test
    @Order(9)
    void shouldUpdateEmployee() {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-UPD-001").fullName("Old Name").email("old@t.test").active(true).build());

        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP-UPD-001")
                .fullName("New Name")
                .email("new@t.test")
                .department("Updated")
                .build();

        EmployeeResponse response = employeeService.update(saved.getId(), request);

        assertEquals("New Name", response.getFullName());
        assertEquals("Updated", response.getDepartment());
    }

    @Test
    @Order(10)
    void shouldDeactivateEmployee() {
        Employee saved = employeeRepository.save(Employee.builder()
                .employeeNumber("EMP-DEAC-001").fullName("To Deactivate").email("deac@t.test").active(true).build());

        employeeService.deactivate(saved.getId());

        Employee updated = employeeRepository.findById(saved.getId()).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @Order(11)
    void shouldThrowWhenUpdatingNonExistentEmployee() {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP-NOEX").fullName("No").email("no@t.test").build();

        assertThrows(ResourceNotFoundException.class, () -> employeeService.update(99999L, request));
    }

    @Test
    @Order(12)
    void shouldThrowWhenDeactivatingNonExistentEmployee() {
        assertThrows(ResourceNotFoundException.class, () -> employeeService.deactivate(99999L));
    }
}

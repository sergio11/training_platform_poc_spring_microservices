package com.poc.training.employee.service;

import com.poc.training.employee.dto.EmployeeRequest;
import com.poc.training.employee.dto.EmployeeResponse;
import com.poc.training.employee.model.Employee;
import com.poc.training.employee.repository.EmployeeRepository;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void shouldFindAllActiveEmployees() {
        Employee e1 = Employee.builder().id(1L).employeeNumber("EMP001").fullName("Alice").email("alice@test.com").active(true).build();
        Employee e2 = Employee.builder().id(2L).employeeNumber("EMP002").fullName("Bob").email("bob@test.com").active(true).build();
        when(employeeRepository.findByActiveTrue(any(PageRequest.class))).thenReturn(new PageImpl<>(Arrays.asList(e1, e2)));

        var result = employeeService.findAll(PageRequest.of(0, 10));
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldFindEmployeeById() {
        Employee e = Employee.builder().id(1L).employeeNumber("EMP001").fullName("Alice").email("alice@test.com").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(e));

        EmployeeResponse result = employeeService.findById(1L);
        assertEquals("Alice", result.getFullName());
    }

    @Test
    void shouldThrowWhenEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> employeeService.findById(99L));
    }

    @Test
    void shouldCreateEmployee() {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP003")
                .fullName("Charlie")
                .email("charlie@test.com")
                .department("Engineering")
                .build();

        when(employeeRepository.findByEmployeeNumber("EMP003")).thenReturn(Optional.empty());
        when(employeeRepository.findByEmail("charlie@test.com")).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });

        EmployeeResponse result = employeeService.create(request);
        assertEquals("EMP003", result.getEmployeeNumber());
        assertEquals("Charlie", result.getFullName());
    }

    @Test
    void shouldThrowWhenEmployeeNumberAlreadyExists() {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .build();

        when(employeeRepository.findByEmployeeNumber("EMP001")).thenReturn(Optional.of(Employee.builder().id(1L).build()));

        assertThrows(BusinessException.class, () -> employeeService.create(request));
    }

    @Test
    void shouldDeactivateEmployee() {
        Employee e = Employee.builder().id(1L).employeeNumber("EMP001").fullName("Alice").email("alice@test.com").active(true).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(e));

        employeeService.deactivate(1L);

        assertFalse(e.isActive());
        verify(employeeRepository).save(e);
    }
}

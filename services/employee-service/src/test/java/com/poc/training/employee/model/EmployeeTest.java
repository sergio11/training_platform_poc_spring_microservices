package com.poc.training.employee.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {

    @Test
    void shouldSetDefaultValuesOnPrePersist() {
        Employee employee = Employee.builder()
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .build();

        employee.onCreate();

        assertNotNull(employee.getCreatedAt());
        assertNotNull(employee.getUpdatedAt());
        assertEquals(employee.getCreatedAt(), employee.getUpdatedAt());
    }

    @Test
    void shouldUpdateTimestampOnPreUpdate() {
        Employee employee = Employee.builder()
                .id(1L)
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        employee.onUpdate();

        assertNotNull(employee.getUpdatedAt());
        assertTrue(employee.getUpdatedAt().isAfter(employee.getCreatedAt()));
    }

    @Test
    void shouldCreateEmployeeWithBuilder() {
        Employee employee = Employee.builder()
                .id(1L)
                .keycloakUserId("kc-123")
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .department("Engineering")
                .position("Developer")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertEquals(1L, employee.getId());
        assertEquals("kc-123", employee.getKeycloakUserId());
        assertEquals("EMP001", employee.getEmployeeNumber());
        assertEquals("Alice", employee.getFullName());
        assertEquals("alice@test.com", employee.getEmail());
        assertEquals("Engineering", employee.getDepartment());
        assertEquals("Developer", employee.getPosition());
        assertTrue(employee.isActive());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        Employee employee = new Employee();
        assertNotNull(employee);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        Employee employee = new Employee(1L, "kc-123", "EMP001", "Alice",
                "alice@test.com", "Engineering", "Developer", true,
                LocalDateTime.now(), LocalDateTime.now());
        assertEquals("EMP001", employee.getEmployeeNumber());
    }

    @Test
    void shouldDefaultActiveToTrue() {
        Employee employee = Employee.builder()
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .build();

        assertTrue(employee.isActive());
    }

    @Test
    void shouldHaveCorrectToString() {
        Employee employee = Employee.builder()
                .id(1L)
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .build();

        String str = employee.toString();
        assertNotNull(str);
        assertTrue(str.contains("EMP001"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        Employee e1 = Employee.builder()
                .id(1L).employeeNumber("EMP001").fullName("Alice").email("alice@test.com").build();
        Employee e2 = Employee.builder()
                .id(1L).employeeNumber("EMP002").fullName("Bob").email("bob@test.com").build();
        Employee e3 = Employee.builder()
                .id(2L).employeeNumber("EMP001").fullName("Alice").email("alice@test.com").build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
    }

    @Test
    void shouldSetAndGetAllSetters() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setKeycloakUserId("kc-123");
        employee.setEmployeeNumber("EMP001");
        employee.setFullName("Alice");
        employee.setEmail("alice@test.com");
        employee.setDepartment("Engineering");
        employee.setPosition("Developer");
        employee.setActive(true);

        assertEquals(1L, employee.getId());
        assertEquals("kc-123", employee.getKeycloakUserId());
        assertEquals("EMP001", employee.getEmployeeNumber());
        assertEquals("Alice", employee.getFullName());
        assertEquals("alice@test.com", employee.getEmail());
        assertEquals("Engineering", employee.getDepartment());
        assertEquals("Developer", employee.getPosition());
        assertTrue(employee.isActive());
    }
}

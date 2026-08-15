package com.poc.training.employee.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeRequestTest {

    @Test
    void shouldCreateWithBuilder() {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .department("Engineering")
                .position("Developer")
                .keycloakUserId("kc-123")
                .build();

        assertEquals("EMP001", request.getEmployeeNumber());
        assertEquals("Alice", request.getFullName());
        assertEquals("alice@test.com", request.getEmail());
        assertEquals("Engineering", request.getDepartment());
        assertEquals("Developer", request.getPosition());
        assertEquals("kc-123", request.getKeycloakUserId());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        EmployeeRequest request = new EmployeeRequest();
        assertNotNull(request);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        EmployeeRequest request = new EmployeeRequest("EMP001", "Alice",
                "alice@test.com", "Engineering", "Developer", "kc-123");
        assertEquals("EMP001", request.getEmployeeNumber());
    }

    @Test
    void shouldHaveCorrectToString() {
        EmployeeRequest request = EmployeeRequest.builder()
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .build();

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("EMP001"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        EmployeeRequest r1 = EmployeeRequest.builder()
                .employeeNumber("EMP001").fullName("Alice").email("alice@test.com").build();
        EmployeeRequest r2 = EmployeeRequest.builder()
                .employeeNumber("EMP001").fullName("Alice").email("alice@test.com").build();
        EmployeeRequest r3 = EmployeeRequest.builder()
                .employeeNumber("EMP002").fullName("Bob").email("bob@test.com").build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }
}

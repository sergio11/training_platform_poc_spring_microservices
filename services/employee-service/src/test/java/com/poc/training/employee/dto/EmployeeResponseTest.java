package com.poc.training.employee.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeResponseTest {

    @Test
    void shouldCreateWithBuilder() {
        LocalDateTime now = LocalDateTime.now();
        EmployeeResponse response = EmployeeResponse.builder()
                .id(1L)
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .department("Engineering")
                .position("Developer")
                .keycloakUserId("kc-123")
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, response.getId());
        assertEquals("EMP001", response.getEmployeeNumber());
        assertEquals("Alice", response.getFullName());
        assertEquals("alice@test.com", response.getEmail());
        assertEquals("Engineering", response.getDepartment());
        assertEquals("Developer", response.getPosition());
        assertEquals("kc-123", response.getKeycloakUserId());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        EmployeeResponse response = new EmployeeResponse();
        assertNotNull(response);
    }

    @Test
    void shouldHaveCorrectToString() {
        EmployeeResponse response = EmployeeResponse.builder()
                .id(1L)
                .employeeNumber("EMP001")
                .fullName("Alice")
                .email("alice@test.com")
                .build();

        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("EMP001"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        EmployeeResponse r1 = EmployeeResponse.builder()
                .id(1L).employeeNumber("EMP001").fullName("Alice")
                .email("alice@test.com").active(true).createdAt(now).build();
        EmployeeResponse r2 = EmployeeResponse.builder()
                .id(1L).employeeNumber("EMP001").fullName("Alice")
                .email("alice@test.com").active(true).createdAt(now).build();
        EmployeeResponse r3 = EmployeeResponse.builder()
                .id(2L).employeeNumber("EMP002").fullName("Bob")
                .email("bob@test.com").active(false).createdAt(now).build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }
}

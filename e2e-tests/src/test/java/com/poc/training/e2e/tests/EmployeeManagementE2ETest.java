package com.poc.training.e2e.tests;

import com.poc.training.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Employee Management CRUD")
class EmployeeManagementE2ETest extends BaseE2ETest {

    private static Long employeeId;

    @Test
    @Order(1)
    @DisplayName("Admin can create an employee")
    void adminCreatesEmployee() {
        employeeId = createEmployee(adminToken(), "EMP-MGMT-001");
        assertThat(employeeId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Get employee by ID returns correct data")
    void getEmployeeById() {
        Map<?, ?> emp = employeeClient.get()
                .uri("/api/v1/employees/{id}", employeeId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        assertThat(emp).isNotNull();
        assertThat(emp.get("employeeNumber")).isEqualTo("EMP-MGMT-001");
        assertThat(emp.get("active")).isEqualTo(true);
    }

    @Test
    @Order(3)
    @DisplayName("Get employee by employee number")
    void getEmployeeByNumber() {
        Map<?, ?> emp = employeeClient.get()
                .uri("/api/v1/employees/number/{number}", "EMP-MGMT-001")
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        assertThat(emp).isNotNull();
        assertThat(emp.get("fullName")).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Check employee exists by ID")
    void checkEmployeeExists() {
        Boolean exists = employeeClient.get()
                .uri("/api/v1/employees/{id}/exists", employeeId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Boolean.class).block();

        assertThat(exists).isTrue();

        Boolean notExists = employeeClient.get()
                .uri("/api/v1/employees/{id}/exists", 999999)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Boolean.class).block();

        assertThat(notExists).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("List employees with pagination")
    void listEmployeesPagination() {
        createEmployee(adminToken(), "EMP-MGMT-002");
        createEmployee(adminToken(), "EMP-MGMT-003");

        Map<?, ?> page = employeeClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees")
                        .queryParam("page", 0)
                        .queryParam("size", 2).build())
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();

        assertThat(page).isNotNull();
        assertThat(page.get("content")).isNotNull();
        assertThat(((java.util.List<?>) page.get("content")).size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(6)
    @DisplayName("Admin can update an employee")
    void adminUpdatesEmployee() {
        employeeClient.put()
                .uri("/api/v1/employees/{id}", employeeId)
                .header("Authorization", "Bearer " + adminToken())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("employeeNumber", "EMP-MGMT-001", "fullName", "Updated Name",
                        "email", "updated@e2e.test", "department", "Updated"))
                .retrieve().toBodilessEntity().block();

        Map<?, ?> emp = employeeClient.get()
                .uri("/api/v1/employees/{id}", employeeId)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().bodyToMono(Map.class).block();
        assertThat(emp.get("fullName")).isEqualTo("Updated Name");
    }

    @Test
    @Order(7)
    @DisplayName("Cannot create employee with duplicate number")
    void shouldRejectDuplicateEmployeeNumber() {
        assertThatThrownBy(() -> createEmployee(adminToken(), "EMP-MGMT-001"))
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isIn(400, 409));
    }

    @Test
    @Order(8)
    @DisplayName("Non-existent employee returns 404")
    void return404ForNonExistentEmployee() {
        assertThatThrownBy(() -> employeeClient.get()
                .uri("/api/v1/employees/{id}", 999999)
                .header("Authorization", "Bearer " + adminToken())
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @Order(9)
    @DisplayName("Instructor cannot create employees")
    void instructorCannotCreateEmployee() {
        assertThatThrownBy(() -> employeeClient.post()
                .uri("/api/v1/employees")
                .header("Authorization", "Bearer " + instructorToken())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("employeeNumber", "EMP-FAIL", "fullName", "Fail",
                        "email", "fail@e2e.test"))
                .retrieve().toBodilessEntity().block())
                .isInstanceOf(WebClientResponseException.class)
                .satisfies(e -> assertThat(((WebClientResponseException) e)
                        .getStatusCode().value()).isEqualTo(403));
    }
}

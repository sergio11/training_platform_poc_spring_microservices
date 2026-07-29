package com.poc.training.employee.service;

import tools.jackson.databind.ObjectMapper;
import com.poc.training.employee.dto.EmployeeRequest;
import com.poc.training.employee.dto.EmployeeResponse;
import com.poc.training.employee.model.Employee;
import com.poc.training.employee.model.OutboxEvent;
import com.poc.training.employee.repository.EmployeeRepository;
import com.poc.training.employee.repository.OutboxEventRepository;
import com.poc.training.shared.event.EmployeeCreatedEvent;
import com.poc.training.shared.event.EmployeeDeactivatedEvent;
import com.poc.training.shared.event.EmployeeUpdatedEvent;
import com.poc.training.shared.event.RabbitMQConstants;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public static EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .keycloakUserId(employee.getKeycloakUserId())
                .active(employee.isActive())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> findAll(Pageable pageable) {
        return employeeRepository.findByActiveTrue(pageable).map(EmployeeService::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findByEmployeeNumber(String employeeNumber) {
        Employee employee = employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with number: " + employeeNumber));
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return employeeRepository.existsById(id);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.findByEmployeeNumber(request.getEmployeeNumber()).isPresent()) {
            throw new BusinessException("Employee already exists with number: " + request.getEmployeeNumber());
        }
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Employee already exists with email: " + request.getEmail());
        }

        Employee employee = Employee.builder()
                .employeeNumber(request.getEmployeeNumber())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .department(request.getDepartment())
                .position(request.getPosition())
                .keycloakUserId(request.getKeycloakUserId())
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Created employee: {} ({})", saved.getFullName(), saved.getEmployeeNumber());

        EmployeeCreatedEvent event = EmployeeCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("employee-service")
                .employeeId(saved.getId())
                .employeeNumber(saved.getEmployeeNumber())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .department(saved.getDepartment())
                .build();
        persistOutboxEvent("Employee", String.valueOf(saved.getId()), "EmployeeCreatedEvent",
                event, RabbitMQConstants.ROUTING_KEY_EMPLOYEE_CREATED);

        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());

        Employee saved = employeeRepository.save(employee);
        log.info("Updated employee: {} ({})", saved.getFullName(), saved.getEmployeeNumber());

        EmployeeUpdatedEvent event = EmployeeUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("employee-service")
                .employeeId(saved.getId())
                .employeeNumber(saved.getEmployeeNumber())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .department(saved.getDepartment())
                .position(saved.getPosition())
                .build();
        persistOutboxEvent("Employee", String.valueOf(saved.getId()), "EmployeeUpdatedEvent",
                event, RabbitMQConstants.ROUTING_KEY_EMPLOYEE_UPDATED);

        return toResponse(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employee.setActive(false);
        employeeRepository.save(employee);
        log.info("Deactivated employee: {} ({})", employee.getFullName(), employee.getEmployeeNumber());

        EmployeeDeactivatedEvent event = EmployeeDeactivatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .source("employee-service")
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .build();
        persistOutboxEvent("Employee", String.valueOf(employee.getId()), "EmployeeDeactivatedEvent",
                event, RabbitMQConstants.ROUTING_KEY_EMPLOYEE_DEACTIVATED);
    }

    private void persistOutboxEvent(String aggregateType, String aggregateId, String eventType, Object event, String routingKey) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(event))
                    .routingKey(routingKey)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize {} for outbox: {}", eventType, e.getMessage());
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }
}
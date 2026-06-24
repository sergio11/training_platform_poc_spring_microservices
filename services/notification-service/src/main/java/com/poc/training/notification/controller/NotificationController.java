package com.poc.training.notification.controller;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByRecipientId(
            @RequestParam String recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !recipientId.equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own notifications");
        }
        return ResponseEntity.ok(notificationService.findByRecipientId(recipientId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        NotificationResponse notification = notificationService.findById(id);
        Jwt jwt = getJwtFromContext();
        if (isEmployeeOnly(jwt) && !notification.getRecipientId().equals(extractEmployeeId(jwt))) {
            throw new AccessDeniedException("Employees can only view their own notifications");
        }
        return ResponseEntity.ok(notification);
    }

    private Jwt getJwtFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            return (Jwt) auth.getPrincipal();
        }
        return null;
    }

    private boolean isEmployeeOnly(Jwt jwt) {
        if (jwt == null) return false;
        java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) return false;
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
        return roles.contains("employee") && !roles.contains("admin") && !roles.contains("instructor");
    }

    private String extractEmployeeId(Jwt jwt) {
        if (jwt == null) return null;
        java.util.List<String> employeeIds = jwt.getClaimAsStringList("employeeId");
        if (employeeIds != null && !employeeIds.isEmpty()) {
            return employeeIds.get(0);
        }
        return null;
    }
}

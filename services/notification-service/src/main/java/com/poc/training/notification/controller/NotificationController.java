package com.poc.training.notification.controller;

import com.poc.training.notification.dto.NotificationResponse;
import com.poc.training.notification.service.NotificationService;
import com.poc.training.shared.exception.BusinessException;
import com.poc.training.shared.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "type", "recipientId", "message", "referenceId",
        "referenceType", "sentAt", "status"
    );

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByRecipientId(
            @RequestParam Long recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !recipientId.equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own notifications");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessException("Invalid sort field: '" + sortBy + "'. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }
        return ResponseEntity.ok(notificationService.findByRecipientId(recipientId, PageRequest.of(page, size, Sort.by(sortBy))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('employee', 'instructor', 'admin')")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        NotificationResponse notification = notificationService.findById(id);
        Jwt jwt = JwtUtils.getJwtFromContext();
        if (JwtUtils.isEmployeeOnly(jwt) && !notification.getRecipientId().equals(JwtUtils.extractEmployeeIdAsLong(jwt))) {
            throw new AccessDeniedException("Employees can only view their own notifications");
        }
        return ResponseEntity.ok(notification);
    }
}

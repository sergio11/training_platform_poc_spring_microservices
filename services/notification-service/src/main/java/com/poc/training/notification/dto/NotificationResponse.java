package com.poc.training.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private Long recipientId;
    private String message;
    private String referenceId;
    private String referenceType;
    private LocalDateTime sentAt;
    private String status;
}

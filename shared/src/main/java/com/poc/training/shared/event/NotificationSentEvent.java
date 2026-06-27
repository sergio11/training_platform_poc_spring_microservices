package com.poc.training.shared.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationSentEvent extends BaseEvent {
    private Long notificationId;
    private Long recipientId;
    private String notificationType;
    private String referenceId;
    private String referenceType;
}

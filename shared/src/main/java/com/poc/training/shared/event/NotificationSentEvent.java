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
    private String recipientId;
    private String type;
    private String message;
}

package com.poc.training.notification.sender;

import com.poc.training.notification.model.Notification;

public interface NotificationSender {
    boolean send(Notification notification);
}

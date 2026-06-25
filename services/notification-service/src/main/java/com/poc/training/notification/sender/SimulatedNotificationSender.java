package com.poc.training.notification.sender;

import com.poc.training.notification.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test", "default"})
public class SimulatedNotificationSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        log.info("[SIMULATED {}] To: {}, Message: {}",
                notification.getType(),
                notification.getRecipientId(),
                notification.getMessage());
        return true;
    }
}

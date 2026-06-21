package com.poc.training.notification.repository;

import com.poc.training.notification.model.Notification;
import com.poc.training.notification.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);
    List<Notification> findByType(NotificationType type);
    List<Notification> findByReferenceIdAndReferenceType(String referenceId, String referenceType);
}

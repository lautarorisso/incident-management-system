package com.lautarorisso.notification_service.domain.port.out;

import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven port (outbound) for persisting and retrieving Notifications.
 */
public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(NotificationId id);

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);

    List<Notification> findAll();
}

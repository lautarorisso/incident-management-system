package com.lautarorisso.notification_service.repository;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.entity.NotificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link Notification} documents.
 */
public interface NotificationRepository extends MongoRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status);
}

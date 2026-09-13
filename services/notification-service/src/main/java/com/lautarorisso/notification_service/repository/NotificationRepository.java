package com.lautarorisso.notification_service.repository;

import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link Notification} documents.
 */
public interface NotificationRepository extends MongoRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status);

    /**
     * True when a notification for the given user was already created from the
     * given outbox event id. The redelivery pre-check: with the default
     * prefetch/concurrency of 1, checking before creating is race-free.
     * A unique index on {@code eventId} is intentionally avoided — legacy
     * notifications carry null eventIds, and the pre-check IS the dedup mechanism.
     */
    boolean existsByEventIdAndUserId(String eventId, UUID userId);
}

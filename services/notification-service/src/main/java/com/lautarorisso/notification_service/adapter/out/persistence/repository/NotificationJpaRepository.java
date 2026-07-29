package com.lautarorisso.notification_service.adapter.out.persistence.repository;

import com.lautarorisso.notification_service.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for NotificationEntity.
 */
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<NotificationEntity> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
}

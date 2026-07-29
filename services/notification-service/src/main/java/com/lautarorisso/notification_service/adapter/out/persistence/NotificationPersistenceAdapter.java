package com.lautarorisso.notification_service.adapter.out.persistence;

import com.lautarorisso.notification_service.adapter.out.persistence.mapper.NotificationEntityMapper;
import com.lautarorisso.notification_service.adapter.out.persistence.repository.NotificationJpaRepository;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.port.out.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter implementing NotificationRepository.
 */
@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationEntityMapper mapper;

    @Override
    @Transactional
    public Notification save(Notification notification) {
        var entity = mapper.toEntity(notification);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(NotificationId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status) {
        return jpaRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, status.name()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

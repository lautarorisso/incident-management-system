package com.lautarorisso.notification_service.adapter.out.persistence.repository;

import com.lautarorisso.notification_service.adapter.out.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for ProcessedEventEntity.
 */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, String> {
}

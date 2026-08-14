package com.lautarorisso.notification_service.repository;

import com.lautarorisso.notification_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link ProcessedEvent} entities.
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}

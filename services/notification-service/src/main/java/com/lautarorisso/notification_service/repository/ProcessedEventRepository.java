package com.lautarorisso.notification_service.repository;

import com.lautarorisso.notification_service.entity.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data repository for {@link ProcessedEvent} documents.
 */
public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
}

package com.lautarorisso.notification_service.domain.port.out;

import com.lautarorisso.notification_service.domain.model.ProcessedEvent;

import java.util.Optional;

/**
 * Driven port (outbound) for tracking processed events for idempotency.
 */
public interface ProcessedEventRepository {

    ProcessedEvent save(ProcessedEvent processedEvent);

    Optional<ProcessedEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}

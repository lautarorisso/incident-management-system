package com.lautarorisso.incident_service.adapter.out.messaging;

import com.lautarorisso.incident_service.adapter.persistence.entity.OutboxEventEntity;
import com.lautarorisso.incident_service.adapter.persistence.repository.OutboxEventJpaRepository;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled poller that reads unpublished outbox events, publishes them
 * to RabbitMQ via RabbitMqEventPublisher, and marks them as published.
 * <p>
 * Implements the transactional outbox pattern for reliable event delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventJpaRepository outboxRepo;
    private final RabbitMqEventPublisher eventPublisher;

    /**
     * Processes all unpublished outbox events.
     * Runs every 5 seconds with an initial delay of 10 seconds after startup.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutbox() {
        var unpublished = outboxRepo.findByPublishedFalse();
        if (unpublished.isEmpty()) {
            return;
        }

        log.info("Processing {} unpublished outbox events", unpublished.size());

        for (OutboxEventEntity event : unpublished) {
            try {
                IncidentEvent eventType = IncidentEvent.valueOf(event.getEventType());
                IncidentId incidentId = new IncidentId(event.getAggregateId());

                eventPublisher.publish(eventType, incidentId);

                event.setPublished(true);
                outboxRepo.save(event);

                log.debug("Published outbox event {} for incident {}",
                        event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}",
                        event.getId(), e.getMessage(), e);
            }
        }
    }
}

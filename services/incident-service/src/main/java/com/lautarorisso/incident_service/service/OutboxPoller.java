package com.lautarorisso.incident_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.enums.IncidentEvent;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.messaging.RabbitMqEventPublisher;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled poller that reads unpublished outbox events, publishes them
 * to RabbitMQ via the {@link RabbitMqEventPublisher}, and marks them as published.
 * <p>
 * Implements the transactional outbox pattern for reliable event delivery.
 * <p>
 * Poison-pill handling: every failed publish increments {@code attempts} and
 * records {@code lastError}, so a persistently failing event is retried at most
 * {@value #MAX_ATTEMPTS} times. Events that reach {@code MAX_ATTEMPTS} simply stop
 * being selected by the repository query — a DB-side dead-letter queue: they stay
 * {@code unpublished} and remain inspectable in the outbox_events table.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitMqEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Processes all unpublished outbox events that have not exhausted their retry
     * budget. Runs every 5 seconds with an initial delay of 10 seconds after startup.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalseAndAttemptsLessThan(MAX_ATTEMPTS);
        if (unpublished.isEmpty()) {
            return;
        }

        log.info("Processing {} unpublished outbox events", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                IncidentEvent eventType = IncidentEvent.valueOf(event.getEventType());
                Map<String, Object> eventData = objectMapper.readValue(
                        event.getPayload(), new TypeReference<LinkedHashMap<String, Object>>() {});
                // Stamp a unique event id so consumers can dedupe on it instead of
                // (incidentId + eventType), which collides for repeated events of
                // the same incident.
                eventData.put("eventId", event.getId().toString());

                eventPublisher.publish(eventType, eventData);

                event.setPublished(true);
                outboxEventRepository.save(event);

                log.info("Published outbox event {} for aggregate {}",
                        event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                // A failing event must not abort the batch: record the failure and
                // let the retry budget govern how often it is attempted again.
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(e.getMessage());
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event {} (attempt {} of {}): {}",
                        event.getId(), event.getAttempts(), MAX_ATTEMPTS, e.getMessage());
            }
        }
    }
}

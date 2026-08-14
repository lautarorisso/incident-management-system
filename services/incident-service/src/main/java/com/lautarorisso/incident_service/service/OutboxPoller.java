package com.lautarorisso.incident_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.entity.IncidentEvent;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.messaging.OutboxPollerEventForwarder;
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
 * to RabbitMQ via the {@link OutboxPollerEventForwarder}, and marks them as published.
 * <p>
 * Implements the transactional outbox pattern for reliable event delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPollerEventForwarder forwarder;
    private final ObjectMapper objectMapper;

    /**
     * Processes all unpublished outbox events.
     * Runs every 5 seconds with an initial delay of 10 seconds after startup.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();
        if (unpublished.isEmpty()) {
            return;
        }

        log.info("Processing {} unpublished outbox events", unpublished.size());

        for (OutboxEvent event : unpublished) {
            try {
                IncidentEvent eventType = IncidentEvent.valueOf(event.getEventType());
                Map<String, Object> eventData = objectMapper.readValue(
                        event.getPayload(), new TypeReference<LinkedHashMap<String, Object>>() {});

                forwarder.publish(eventType, eventData);

                event.setPublished(true);
                outboxEventRepository.save(event);

                log.debug("Published outbox event {} for aggregate {}",
                        event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}",
                        event.getId(), e.getMessage(), e);
            }
        }
    }
}

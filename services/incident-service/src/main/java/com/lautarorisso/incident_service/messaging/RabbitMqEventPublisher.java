package com.lautarorisso.incident_service.messaging;

import com.lautarorisso.incident_service.entity.IncidentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * RabbitMQ implementation of the {@link OutboxPollerEventForwarder}.
 * <p>
 * Converts outbox event payloads to messages and publishes them
 * to the "incident.events" exchange.
 */
@Component
@RequiredArgsConstructor
public class RabbitMqEventPublisher implements OutboxPollerEventForwarder {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes an incident event to RabbitMQ.
     *
     * @param eventType the type of domain event
     * @param eventData the event payload as a map (incidentId, assigneeId, teamId, priority, status, title)
     */
    @Override
    public void publish(IncidentEvent eventType, Map<String, Object> eventData) {
        eventData.put("eventType", eventType.name());
        eventData.put("timestamp", Instant.now().toString());

        String routingKey = "incident." + eventType.name().toLowerCase()
                .replace("incident_", "");

        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME, routingKey, eventData);
    }
}

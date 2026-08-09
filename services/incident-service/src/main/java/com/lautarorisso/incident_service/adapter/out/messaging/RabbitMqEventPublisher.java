package com.lautarorisso.incident_service.adapter.out.messaging;

import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * RabbitMQ implementation of the {@link EventPublisher} port.
 * <p>
 * Converts domain events to messages and publishes them to the "incident.events" exchange.
 * Used by the OutboxPoller to forward persisted outbox events to the broker.
 */
@Component
@RequiredArgsConstructor
public class RabbitMqEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes an incident event to RabbitMQ.
     *
     * @param eventType the type of domain event
     * @param incidentId the incident ID with full context for the event payload
     */
    @Override
    public void publish(IncidentEvent eventType, IncidentId incidentId) {
        String routingKey = routingKeyFor(eventType);
        EventMessage message = new EventMessage(
                incidentId.getValue(),
                eventType.name(),
                Instant.now(),
                null, // assigneeId not available from IncidentId
                null, // teamId not available from IncidentId
                null, // priority not available from IncidentId
                null  // status not available from IncidentId
        );
        rabbitTemplate.convertAndSend("incident.events", routingKey, message);
    }

    private String routingKeyFor(IncidentEvent eventType) {
        return "incident." + eventType.name().toLowerCase()
                .replace("incident_", "");
    }

    /**
     * Event message payload sent to RabbitMQ.
     */
    public record EventMessage(
            UUID incidentId,
            String eventType,
            Instant timestamp,
            UUID assigneeId,
            UUID teamId,
            String priority,
            String status
    ) {}
}

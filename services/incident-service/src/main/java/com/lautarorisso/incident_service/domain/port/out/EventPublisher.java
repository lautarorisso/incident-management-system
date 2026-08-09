package com.lautarorisso.incident_service.domain.port.out;

import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;

/**
 * Driven port (outbound) for forwarding persisted domain events to the
 * message broker.
 * <p>
 * This is the port the {@code OutboxPoller} uses to dispatch outbox events:
 * the application layer writes events to the outbox via
 * {@link IncidentEventPublisher}, and the poller forwards them to the broker
 * through this port. The infrastructure layer provides the implementation
 * (e.g. the RabbitMQ publisher).
 * <p>
 * Keeping this separate from {@link IncidentEventPublisher} avoids two beans
 * of the same port type competing for injection and keeps each adapter's
 * role explicit: persist-to-outbox vs. forward-to-broker.
 */
public interface EventPublisher {

    void publish(IncidentEvent eventType, IncidentId incidentId);
}

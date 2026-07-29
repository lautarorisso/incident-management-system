package com.lautarorisso.incident_service.domain.port.out;

import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;

/**
 * Driven port (outbound) for publishing domain events related to Incidents.
 * <p>
 * The application layer calls this interface; the infrastructure layer
 * provides the implementation (e.g. RabbitMQ publisher or outbox-based publisher).
 */
public interface IncidentEventPublisher {

    void publish(IncidentEvent eventType, IncidentId incidentId);
}

package com.lautarorisso.incident_service.messaging;

import com.lautarorisso.incident_service.entity.IncidentEvent;

import java.util.Map;

/**
 * Port used by {@link com.lautarorisso.incident_service.service.OutboxPoller}
 * to forward outbox events to the message broker.
 */
public interface OutboxPollerEventForwarder {

    void publish(IncidentEvent eventType, Map<String, Object> eventData);
}

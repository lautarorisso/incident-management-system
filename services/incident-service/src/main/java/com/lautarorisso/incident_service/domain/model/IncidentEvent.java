package com.lautarorisso.incident_service.domain.model;

/**
 * Domain event types that can be published when an Incident changes state.
 */
public enum IncidentEvent {
    INCIDENT_CREATED,
    INCIDENT_ASSIGNED,
    INCIDENT_STATUS_CHANGED,
    INCIDENT_PRIORITY_CHANGED,
    INCIDENT_DELETED
}

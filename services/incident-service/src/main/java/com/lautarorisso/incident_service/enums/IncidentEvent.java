package com.lautarorisso.incident_service.enums;

/**
 * Domain event types that can be published when an Incident changes state.
 */
public enum IncidentEvent {
    INCIDENT_CREATED,
    INCIDENT_ASSIGNED,
    INCIDENT_STATUS_CHANGED
}

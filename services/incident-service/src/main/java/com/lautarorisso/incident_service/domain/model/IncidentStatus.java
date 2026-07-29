package com.lautarorisso.incident_service.domain.model;

/**
 * Lifecycle states for an Incident.
 * <p>
 * Valid state-machine progression:
 * OPEN → IN_PROGRESS → RESOLVED → CLOSED
 * <p>
 * A resolved incident may also be reopened (RESOLVED → OPEN).
 */
public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

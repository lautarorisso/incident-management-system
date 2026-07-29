package com.lautarorisso.incident_service.domain.port.in;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;

/**
 * Driving port (inbound) for transitioning an Incident through its
 * state machine: OPEN → IN_PROGRESS → RESOLVED → CLOSED (with reopen).
 * <p>
 * Implementations validate transitions via domain service, persist,
 * and publish events.
 */
public interface TransitionIncidentUseCase {

    /**
     * Transitions an incident to the given new status.
     *
     * @param incidentId the incident to transition
     * @param newStatus  the target status
     * @return the updated Incident
     * @throws IllegalArgumentException if incident not found
     * @throws IllegalStateException    if the transition is invalid
     */
    Incident transitionIncident(IncidentId incidentId, IncidentStatus newStatus);
}

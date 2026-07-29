package com.lautarorisso.incident_service.domain.port.in;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;

import java.util.UUID;

/**
 * Driving port (inbound) for assigning an Incident to a user and/or team.
 * <p>
 * Implementations validate user/team existence via Feign client, apply
 * domain rules, persist, and publish events.
 */
public interface AssignIncidentUseCase {

    /**
     * Assigns an incident to the specified user and/or team.
     *
     * @param incidentId the incident to assign
     * @param assigneeId the user to assign
     * @param teamId     the team to assign (nullable)
     * @return the updated Incident
     * @throws IllegalArgumentException if incident not found or assignment is invalid
     */
    Incident assignIncident(IncidentId incidentId, UUID assigneeId, UUID teamId);
}

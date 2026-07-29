package com.lautarorisso.incident_service.domain.port.in;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;

import java.util.Optional;

/**
 * Driving port (inbound) for retrieving a single Incident by ID.
 */
public interface GetIncidentUseCase {

    /**
     * Finds an incident by its unique identifier.
     *
     * @param incidentId the incident ID
     * @return an Optional containing the incident if found, empty otherwise
     */
    Optional<Incident> getIncident(IncidentId incidentId);
}

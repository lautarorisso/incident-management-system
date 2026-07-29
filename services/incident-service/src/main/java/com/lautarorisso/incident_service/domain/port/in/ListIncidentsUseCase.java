package com.lautarorisso.incident_service.domain.port.in;

import com.lautarorisso.incident_service.domain.model.Incident;

import java.util.List;

/**
 * Driving port (inbound) for listing all Incidents.
 */
public interface ListIncidentsUseCase {

    /**
     * Returns all incidents.
     *
     * @return list of all incidents (empty list if none)
     */
    List<Incident> listIncidents();
}

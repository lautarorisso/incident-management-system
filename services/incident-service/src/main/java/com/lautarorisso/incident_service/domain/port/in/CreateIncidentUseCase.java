package com.lautarorisso.incident_service.domain.port.in;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;

/**
 * Driving port (inbound) for creating a new Incident.
 * <p>
 * Implementations orchestrate validation, domain service rules, persistence,
 * and event publishing.
 */
public interface CreateIncidentUseCase {

    /**
     * Creates a new incident with the given details.
     *
     * @param title       incident title
     * @param description incident description (nullable)
     * @param priority    incident priority (if null, defaults to MEDIUM)
     * @return the created Incident with generated id and timestamps
     */
    Incident createIncident(String title, String description, IncidentPriority priority);
}

package com.lautarorisso.incident_service.domain.port.out;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (outbound) for persisting and retrieving Incidents.
 * <p>
 * The application layer calls this interface; the infrastructure layer
 * provides the implementation (e.g. JPA adapter).
 */
public interface IncidentRepository {

    Incident save(Incident incident);

    Optional<Incident> findById(IncidentId id);

    List<Incident> findAll();

    void deleteById(IncidentId id);
}

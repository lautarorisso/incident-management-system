package com.lautarorisso.incident_service.domain.port.out;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Returns a page of incidents filtered by the provided criteria and
     * ordered by {@code createdAt} descending.
     *
     * @param status    optional status filter (e.g. OPEN, IN_PROGRESS), null/blank to ignore
     * @param priority  optional priority filter, null/blank to ignore
     * @param assigneeId optional assignee UUID filter, null to ignore
     * @param teamId    optional team UUID filter, null to ignore
     * @param pageable  pagination and sorting information
     * @return page of matching incidents
     */
    Page<Incident> findIncidents(String status, String priority, UUID assigneeId, UUID teamId, Pageable pageable);

    void deleteById(IncidentId id);
}

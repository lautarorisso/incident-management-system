package com.lautarorisso.incident_service.domain.port.in;

import com.lautarorisso.incident_service.domain.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Driving port (inbound) for listing Incidents with filtering and pagination.
 */
public interface ListIncidentsUseCase {

    /**
     * Returns a page of incidents matching the provided filters.
     *
     * @param status     optional status filter (e.g. OPEN, IN_PROGRESS), null/blank to ignore
     * @param priority   optional priority filter, null/blank to ignore
     * @param assigneeId optional assignee UUID filter, null to ignore
     * @param teamId     optional team UUID filter, null to ignore
     * @param pageable   pagination and sorting information
     * @return page of matching incidents (content empty if none)
     */
    Page<Incident> listIncidents(String status, String priority,
                                 UUID assigneeId, UUID teamId, Pageable pageable);
}

package com.lautarorisso.incident_service.application.service;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.port.in.ListIncidentsUseCase;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: list incidents with optional filters and pagination.
 */
@Component
@RequiredArgsConstructor
public class ListIncidentsUseCaseImpl implements ListIncidentsUseCase {

    private final IncidentRepository incidentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Incident> listIncidents(String status, String priority,
                                        UUID assigneeId, UUID teamId, Pageable pageable) {
        return incidentRepository.findIncidents(status, priority, assigneeId, teamId, pageable);
    }
}

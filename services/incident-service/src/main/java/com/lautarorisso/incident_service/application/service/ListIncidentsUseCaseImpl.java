package com.lautarorisso.incident_service.application.service;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.port.in.ListIncidentsUseCase;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case: list all incidents.
 */
@Component
@RequiredArgsConstructor
public class ListIncidentsUseCaseImpl implements ListIncidentsUseCase {

    private final IncidentRepository incidentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Incident> listIncidents() {
        return incidentRepository.findAll();
    }
}

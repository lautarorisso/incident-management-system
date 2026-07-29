package com.lautarorisso.incident_service.application.service;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.port.in.GetIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case: retrieve a single incident by ID.
 */
@Component
@RequiredArgsConstructor
public class GetIncidentUseCaseImpl implements GetIncidentUseCase {

    private final IncidentRepository incidentRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> getIncident(IncidentId incidentId) {
        return incidentRepository.findById(incidentId);
    }
}

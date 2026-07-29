package com.lautarorisso.incident_service.application.service;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import com.lautarorisso.incident_service.domain.port.in.TransitionIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.out.IncidentEventPublisher;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import com.lautarorisso.incident_service.domain.service.IncidentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: transition an incident through its state machine.
 * <p>
 * Validates the transition via IncidentDomainService, persists,
 * and publishes an INCIDENT_STATUS_CHANGED event.
 */
@Component
@RequiredArgsConstructor
public class TransitionIncidentUseCaseImpl implements TransitionIncidentUseCase {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher eventPublisher;
    private final IncidentDomainService domainService;

    @Override
    @Transactional
    public Incident transitionIncident(IncidentId incidentId, IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must not be null");
        }

        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId.getValue()));

        var updated = domainService.changeStatus(incident, newStatus);
        var saved = incidentRepository.save(updated);
        eventPublisher.publish(IncidentEvent.INCIDENT_STATUS_CHANGED, saved.getId());

        return saved;
    }
}

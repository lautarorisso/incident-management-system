package com.lautarorisso.incident_service.application.service;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.port.in.CreateIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.out.IncidentEventPublisher;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: create a new incident.
 * <p>
 * Validates input, builds the domain model, persists via repository,
 * and publishes an INCIDENT_CREATED event.
 */
@Component
@RequiredArgsConstructor
public class CreateIncidentUseCaseImpl implements CreateIncidentUseCase {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher eventPublisher;

    @Override
    @Transactional
    public Incident createIncident(String title, String description, IncidentPriority priority) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }

        IncidentPriority resolvedPriority = priority != null ? priority : IncidentPriority.MEDIUM;

        var incident = Incident.builder()
                .id(new IncidentId(UUID.randomUUID()))
                .title(title.trim())
                .description(description != null ? description.trim() : null)
                .status(com.lautarorisso.incident_service.domain.model.IncidentStatus.OPEN)
                .priority(resolvedPriority)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        var saved = incidentRepository.save(incident);
        eventPublisher.publish(IncidentEvent.INCIDENT_CREATED, saved.getId());

        return saved;
    }
}

package com.lautarorisso.incident_service.application.service;

import com.lautarorisso.incident_service.adapter.out.feign.UserServiceClient;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.port.in.AssignIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.out.IncidentEventPublisher;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: assign an incident to a user and/or team.
 * <p>
 * Validates the user/team existence via Feign, updates the domain model,
 * persists, and publishes an INCIDENT_ASSIGNED event.
 */
@Component
@RequiredArgsConstructor
public class AssignIncidentUseCaseImpl implements AssignIncidentUseCase {

    private final IncidentRepository incidentRepository;
    private final IncidentEventPublisher eventPublisher;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public Incident assignIncident(IncidentId incidentId, UUID assigneeId, UUID teamId) {
        if (assigneeId == null) {
            throw new IllegalArgumentException("Assignee ID must not be null");
        }

        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId.getValue()));

        // Validate user exists via Feign
        var user = userServiceClient.findUserById(assigneeId);
        if (user == null || !user.active()) {
            throw new IllegalArgumentException("User not found or inactive: " + assigneeId);
        }

        // Validate team if provided
        if (teamId != null) {
            var team = userServiceClient.findTeamById(teamId);
            if (team == null || !team.active()) {
                throw new IllegalArgumentException("Team not found or inactive: " + teamId);
            }
        }

        var updated = Incident.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .status(incident.getStatus())
                .priority(incident.getPriority())
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(incident.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        var saved = incidentRepository.save(updated);
        eventPublisher.publish(IncidentEvent.INCIDENT_ASSIGNED, saved.getId());

        return saved;
    }
}

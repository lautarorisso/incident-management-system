package com.lautarorisso.incident_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ims.shared.exception.NotFoundException;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.enums.IncidentEvent;
import com.lautarorisso.incident_service.enums.IncidentPriority;
import com.lautarorisso.incident_service.enums.IncidentStatus;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.lautarorisso.incident_service.client.UserServiceClient;
import com.ims.shared.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-layer service consolidating all Incident use cases:
 * create, assign, transition, get, and list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    // --- create ---

    @Transactional
    public Incident createIncident(String title, String description, IncidentPriority priority) {
        Incident incident = Incident.open(title, description, priority);

        Incident saved = incidentRepository.save(incident);
        publishOutbox(IncidentEvent.INCIDENT_CREATED, saved, null);

        return saved;
    }

    // --- assign ---

    @Transactional
    public Incident assignIncident(UUID id, UUID assigneeId, UUID teamId) {
        if (assigneeId == null) {
            throw new IllegalArgumentException("Assignee ID must not be null");
        }

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));

        UserDto user = userServiceClient.findUserById(assigneeId);
        if (!user.active()) {
            throw new NotFoundException("User not found or inactive: " + assigneeId);
        }

        if (teamId != null) {
            userServiceClient.findTeamById(teamId);
        }

        incident.assignTo(assigneeId, teamId);

        Incident saved = incidentRepository.save(incident);
        publishOutbox(IncidentEvent.INCIDENT_ASSIGNED, saved, user.email());

        return saved;
    }

    // --- transition ---

    @Transactional
    public Incident transitionIncident(UUID id, IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must not be null");
        }

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));

        incident.changeStatus(newStatus);

        Incident saved = incidentRepository.save(incident);
        String assigneeEmail = resolveAssigneeEmail(incident.getAssigneeId());
        publishOutbox(IncidentEvent.INCIDENT_STATUS_CHANGED, saved, assigneeEmail);

        return saved;
    }

    // --- get ---

    @Transactional(readOnly = true)
    public Optional<Incident> getIncident(UUID id) {
        return incidentRepository.findById(id);
    }

    // --- list ---

    @Transactional(readOnly = true)
    public Page<Incident> listIncidents(IncidentStatus status, IncidentPriority priority,
                                        UUID assigneeId, UUID teamId, Pageable pageable) {
        Specification<Incident> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (assigneeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assigneeId"), assigneeId));
        }
        if (teamId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("teamId"), teamId));
        }
        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return incidentRepository.findAll(spec, pageable);
    }

    // --- outbox ---

    @Transactional
    protected void publishOutbox(IncidentEvent eventType, Incident incident, String assigneeEmail) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateId(incident.getId());
        outboxEvent.setEventType(eventType.name());
        outboxEvent.setPayload(buildPayload(incident, assigneeEmail));
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
    }

    /**
     * Resolves the assignee's email for the event payload. A failed user lookup
     * must NEVER fail the transition: the email is a denormalized convenience
     * for the notification listener (which has no JWT of its own), so a
     * resolution failure degrades to a null email instead. When the incident
     * has no assignee there is nothing to resolve.
     */
    private String resolveAssigneeEmail(UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        try {
            return userServiceClient.findUserById(assigneeId).email();
        } catch (Exception e) {
            log.warn("Could not resolve assignee email for user {}: {}", assigneeId, e.getMessage());
            return null;
        }
    }

    private String buildPayload(Incident incident, String assigneeEmail) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("incidentId", incident.getId().toString());
        node.put("title", incident.getTitle());
        node.put("status", incident.getStatus().name());
        node.put("priority", incident.getPriority().name());
        if (incident.getAssigneeId() != null) {
            node.put("assigneeId", incident.getAssigneeId().toString());
        } else {
            node.putNull("assigneeId");
        }
        if (incident.getTeamId() != null) {
            node.put("teamId", incident.getTeamId().toString());
        } else {
            node.putNull("teamId");
        }
        if (assigneeEmail != null) {
            node.put("assigneeEmail", assigneeEmail);
        } else {
            node.putNull("assigneeEmail");
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}

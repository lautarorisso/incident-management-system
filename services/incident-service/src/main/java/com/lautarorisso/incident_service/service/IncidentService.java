package com.lautarorisso.incident_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lautarorisso.incident_service.exception.NotFoundException;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentEvent;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.lautarorisso.incident_service.client.UserServiceClient;
import com.ims.shared.dto.UserDto;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UserServiceClient userServiceClient;
    private final IncidentStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    // --- create ---

    @Transactional
    public Incident createIncident(String title, String description, IncidentPriority priority) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }

        IncidentPriority resolvedPriority = priority != null ? priority : IncidentPriority.MEDIUM;
        Instant now = Instant.now();

        Incident incident = Incident.builder()
                .id(UUID.randomUUID())
                .title(title.trim())
                .description(description != null ? description.trim() : null)
                .status(IncidentStatus.OPEN)
                .priority(resolvedPriority)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Incident saved = incidentRepository.save(incident);
        publishOutbox(IncidentEvent.INCIDENT_CREATED, saved);

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

        incident.setAssigneeId(assigneeId);
        incident.setTeamId(teamId);
        incident.setUpdatedAt(Instant.now());

        Incident saved = incidentRepository.save(incident);
        publishOutbox(IncidentEvent.INCIDENT_ASSIGNED, saved);

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

        stateMachine.changeStatus(incident, newStatus);

        Incident saved = incidentRepository.save(incident);
        publishOutbox(IncidentEvent.INCIDENT_STATUS_CHANGED, saved);

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
    protected void publishOutbox(IncidentEvent eventType, Incident incident) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateId(incident.getId());
        outboxEvent.setEventType(eventType.name());
        outboxEvent.setPayload(buildPayload(incident));
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
    }

    private String buildPayload(Incident incident) {
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
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}

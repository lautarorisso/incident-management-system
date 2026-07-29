package com.lautarorisso.incident_service.domain.service;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain service encapsulating incident business rules:
 * <ul>
 *   <li>State machine transitions: OPEN → IN_PROGRESS → RESOLVED → CLOSED (with RESOLVED → OPEN reopen)</li>
 *   <li>Priority rules: CRITICAL incidents cannot be downgraded below HIGH</li>
 * </ul>
 * <p>
 * This is a stateless service with no infrastructure dependencies.
 */
public class IncidentDomainService {

    private static final Map<IncidentStatus, Set<IncidentStatus>> VALID_TRANSITIONS = new EnumMap<>(IncidentStatus.class);

    static {
        VALID_TRANSITIONS.put(IncidentStatus.OPEN, EnumSet.of(IncidentStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentStatus.RESOLVED));
        VALID_TRANSITIONS.put(IncidentStatus.RESOLVED, EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.OPEN));
        VALID_TRANSITIONS.put(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));
    }

    /**
     * Transitions an incident to a new status, validating the state machine rules.
     *
     * @param incident the current incident state (immutable, a new copy is returned)
     * @param newStatus the target status
     * @return a new Incident with the updated status and timestamp
     * @throws IllegalArgumentException if newStatus is null
     * @throws IllegalStateException    if the transition is not allowed
     */
    public Incident changeStatus(Incident incident, IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status must not be null");
        }
        if (!isValidTransition(incident.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + incident.getStatus() + " to " + newStatus);
        }

        return Incident.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .status(newStatus)
                .priority(incident.getPriority())
                .assigneeId(incident.getAssigneeId())
                .teamId(incident.getTeamId())
                .createdAt(incident.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Changes the priority of an incident, enforcing business rules.
     * <p>
     * Rule: CRITICAL priority cannot be downgraded below HIGH.
     *
     * @param incident    the current incident state
     * @param newPriority the target priority
     * @return a new Incident with the updated priority and timestamp
     * @throws IllegalArgumentException if newPriority is null, or the priority change is rejected
     */
    public Incident changePriority(Incident incident, IncidentPriority newPriority) {
        if (newPriority == null) {
            throw new IllegalArgumentException("Target priority must not be null");
        }

        IncidentPriority current = incident.getPriority();

        // CRITICAL can only be downgraded to HIGH or stay CRITICAL
        if (current == IncidentPriority.CRITICAL
                && newPriority.ordinal() < IncidentPriority.HIGH.ordinal()) {
            throw new IllegalArgumentException(
                    "Cannot downgrade CRITICAL priority below HIGH");
        }

        return Incident.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .status(incident.getStatus())
                .priority(newPriority)
                .assigneeId(incident.getAssigneeId())
                .teamId(incident.getTeamId())
                .createdAt(incident.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Checks whether a status transition is valid according to the state machine.
     */
    public boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        Set<IncidentStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}

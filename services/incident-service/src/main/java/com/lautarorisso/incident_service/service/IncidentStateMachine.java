package com.lautarorisso.incident_service.service;

import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain service encapsulating incident business rules:
 * <ul>
 *   <li>State machine transitions: OPEN → IN_PROGRESS → RESOLVED → CLOSED (with RESOLVED → OPEN reopen)</li>
 * </ul>
 * <p>
 * This is a stateless service with no infrastructure dependencies.
 */
@Component
public class IncidentStateMachine {

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
     * @param incident   the current incident (modified in place)
     * @param newStatus  the target status
     * @throws IllegalArgumentException if newStatus is null
     * @throws IllegalStateException    if the transition is not allowed
     */
    public void changeStatus(Incident incident, IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status must not be null");
        }
        if (!isValidTransition(incident.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + incident.getStatus() + " to " + newStatus);
        }
        incident.setStatus(newStatus);
        incident.setUpdatedAt(Instant.now());
    }

    /**
     * Checks whether a status transition is valid according to the state machine.
     */
    public boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        Set<IncidentStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}

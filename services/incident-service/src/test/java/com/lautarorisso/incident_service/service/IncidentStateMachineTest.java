package com.lautarorisso.incident_service.service;

import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IncidentStateMachine} — state machine transitions.
 * <p>
 * Valid transitions: OPEN → IN_PROGRESS → RESOLVED → CLOSED
 * Reopen allowed: RESOLVED → OPEN
 * Terminal state: CLOSED
 */
class IncidentStateMachineTest {

    private IncidentStateMachine stateMachine;
    private UUID incidentId;
    private UUID assigneeId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        stateMachine = new IncidentStateMachine();
        incidentId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        teamId = UUID.randomUUID();
    }

    // --- State Machine: Valid Transitions ---

    @Test
    void shouldTransitionFromOpenToInProgress() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertDoesNotThrow(() -> stateMachine.changeStatus(incident, IncidentStatus.IN_PROGRESS));
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
    }

    @Test
    void shouldTransitionFromInProgressToResolved() {
        Incident incident = createIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.MEDIUM);

        assertDoesNotThrow(() -> stateMachine.changeStatus(incident, IncidentStatus.RESOLVED));
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    void shouldTransitionFromResolvedToClosed() {
        Incident incident = createIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM);

        assertDoesNotThrow(() -> stateMachine.changeStatus(incident, IncidentStatus.CLOSED));
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
    }

    @Test
    void shouldReopenFromResolvedToOpen() {
        Incident incident = createIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM);

        assertDoesNotThrow(() -> stateMachine.changeStatus(incident, IncidentStatus.OPEN));
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
    }

    // --- State Machine: Invalid Transitions ---

    @Test
    void shouldNotTransitionFromOpenToClosed() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.CLOSED));
    }

    @Test
    void shouldNotTransitionFromOpenToResolved() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.RESOLVED));
    }

    @Test
    void shouldNotTransitionFromInProgressToOpen() {
        Incident incident = createIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.OPEN));
    }

    @Test
    void shouldNotTransitionFromInProgressToClosed() {
        Incident incident = createIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.CLOSED));
    }

    @Test
    void shouldNotTransitionFromClosedToAnyState() {
        Incident incident = createIncident(IncidentStatus.CLOSED, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.OPEN));
        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.RESOLVED));
    }

    @Test
    void shouldNotTransitionToSameStatus() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> stateMachine.changeStatus(incident, IncidentStatus.OPEN));
    }

    @Test
    void shouldRejectNullTargetStatus() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.changeStatus(incident, null));
    }

    // --- Validation Helper ---

    @Test
    void shouldDetectInvalidTransition() {
        assertFalse(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.CLOSED));
        assertFalse(stateMachine.isValidTransition(IncidentStatus.IN_PROGRESS, IncidentStatus.OPEN));
        assertFalse(stateMachine.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.OPEN));
    }

    @Test
    void shouldDetectValidTransition() {
        assertTrue(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.CLOSED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.OPEN));
    }

    // --- Helpers ---

    private Incident createIncident(IncidentStatus status, IncidentPriority priority) {
        return Incident.builder()
                .id(incidentId)
                .title("Test incident")
                .description("Test description")
                .status(status)
                .priority(priority)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

package com.lautarorisso.incident_service.domain;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import com.lautarorisso.incident_service.domain.service.IncidentDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IncidentDomainService — state machine transitions and priority rules.
 * <p>
 * Valid transitions: OPEN → IN_PROGRESS → RESOLVED → CLOSED
 * Reopen allowed: RESOLVED → OPEN
 * Terminal state: CLOSED
 */
class IncidentDomainServiceTest {

    private IncidentDomainService domainService;
    private IncidentId incidentId;
    private UUID assigneeId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        domainService = new IncidentDomainService();
        incidentId = new IncidentId(UUID.randomUUID());
        assigneeId = UUID.randomUUID();
        teamId = UUID.randomUUID();
    }

    // --- State Machine: Valid Transitions ---

    @Test
    void shouldTransitionFromOpenToInProgress() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        Incident result = domainService.changeStatus(incident, IncidentStatus.IN_PROGRESS);

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void shouldTransitionFromInProgressToResolved() {
        Incident incident = createIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.MEDIUM);

        Incident result = domainService.changeStatus(incident, IncidentStatus.RESOLVED);

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
    }

    @Test
    void shouldTransitionFromResolvedToClosed() {
        Incident incident = createIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM);

        Incident result = domainService.changeStatus(incident, IncidentStatus.CLOSED);

        assertEquals(IncidentStatus.CLOSED, result.getStatus());
    }

    @Test
    void shouldReopenFromResolvedToOpen() {
        Incident incident = createIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM);

        Incident result = domainService.changeStatus(incident, IncidentStatus.OPEN);

        assertEquals(IncidentStatus.OPEN, result.getStatus());
    }

    // --- State Machine: Invalid Transitions ---

    @Test
    void shouldNotTransitionFromOpenToClosed() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.CLOSED));
    }

    @Test
    void shouldNotTransitionFromOpenToResolved() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.RESOLVED));
    }

    @Test
    void shouldNotTransitionFromInProgressToOpen() {
        Incident incident = createIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.OPEN));
    }

    @Test
    void shouldNotTransitionFromInProgressToClosed() {
        Incident incident = createIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.CLOSED));
    }

    @Test
    void shouldNotTransitionFromClosedToAnyState() {
        Incident incident = createIncident(IncidentStatus.CLOSED, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.OPEN));
        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.RESOLVED));
    }

    @Test
    void shouldNotTransitionToSameStatus() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> domainService.changeStatus(incident, IncidentStatus.OPEN));
    }

    @Test
    void shouldRejectNullTargetStatus() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalArgumentException.class,
                () -> domainService.changeStatus(incident, null));
    }

    // --- Priority Rules ---

    @Test
    void shouldAllowPriorityUpgradeToAnyLevel() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.LOW);

        Incident upgraded = domainService.changePriority(incident, IncidentPriority.CRITICAL);

        assertEquals(IncidentPriority.CRITICAL, upgraded.getPriority());
    }

    @Test
    void shouldAllowPriorityDowngradeFromHighToMedium() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.HIGH);

        Incident downgraded = domainService.changePriority(incident, IncidentPriority.MEDIUM);

        assertEquals(IncidentPriority.MEDIUM, downgraded.getPriority());
    }

    @Test
    void shouldNotDowngradeCriticalToLow() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.CRITICAL);

        assertThrows(IllegalArgumentException.class,
                () -> domainService.changePriority(incident, IncidentPriority.LOW));
    }

    @Test
    void shouldNotDowngradeCriticalToMedium() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.CRITICAL);

        assertThrows(IllegalArgumentException.class,
                () -> domainService.changePriority(incident, IncidentPriority.MEDIUM));
    }

    @Test
    void shouldAllowSamePriority() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.CRITICAL);

        Incident result = domainService.changePriority(incident, IncidentPriority.CRITICAL);

        assertEquals(IncidentPriority.CRITICAL, result.getPriority());
    }

    @Test
    void shouldRejectNullTargetPriority() {
        Incident incident = createIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM);

        assertThrows(IllegalArgumentException.class,
                () -> domainService.changePriority(incident, null));
    }

    // --- Validation Helper ---

    @Test
    void shouldDetectInvalidTransition() {
        assertFalse(domainService.isValidTransition(IncidentStatus.OPEN, IncidentStatus.CLOSED));
        assertFalse(domainService.isValidTransition(IncidentStatus.IN_PROGRESS, IncidentStatus.OPEN));
        assertFalse(domainService.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.OPEN));
    }

    @Test
    void shouldDetectValidTransition() {
        assertTrue(domainService.isValidTransition(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS));
        assertTrue(domainService.isValidTransition(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED));
        assertTrue(domainService.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.CLOSED));
        assertTrue(domainService.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.OPEN));
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

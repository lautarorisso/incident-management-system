package com.lautarorisso.incident_service.application;

import com.lautarorisso.incident_service.adapter.out.feign.UserServiceClient;
import com.lautarorisso.incident_service.adapter.out.feign.dto.TeamDto;
import com.lautarorisso.incident_service.adapter.out.feign.dto.UserDto;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import com.lautarorisso.incident_service.domain.port.in.AssignIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.CreateIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.GetIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.ListIncidentsUseCase;
import com.lautarorisso.incident_service.domain.port.in.TransitionIncidentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for use case wiring.
 * <p>
 * Loads the full Spring context (test profile: H2 database, disabled Eureka/Config)
 * and mocks external dependencies (Feign UserServiceClient, RabbitMQ).
 */
@SpringBootTest
@ActiveProfiles("test")
class IncidentUseCaseIntegrationTest {

    @Autowired
    private CreateIncidentUseCase createIncidentUseCase;

    @Autowired
    private AssignIncidentUseCase assignIncidentUseCase;

    @Autowired
    private TransitionIncidentUseCase transitionIncidentUseCase;

    @Autowired
    private GetIncidentUseCase getIncidentUseCase;

    @Autowired
    private ListIncidentsUseCase listIncidentsUseCase;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private UUID assigneeId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        assigneeId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        // Stub Feign responses for all tests that need them
        when(userServiceClient.findUserById(assigneeId))
                .thenReturn(new UserDto(assigneeId, "jdoe", "John", "Doe", "john@example.com", teamId, true));
        when(userServiceClient.findTeamById(teamId))
                .thenReturn(new TeamDto(teamId, "SRE Team", true));
    }

    @Test
    void shouldCreateAndRetrieveIncident() {
        // Create
        Incident created = createIncidentUseCase.createIncident(
                "Integration test incident",
                "Testing the full wiring",
                IncidentPriority.HIGH);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Integration test incident", created.getTitle());
        assertEquals(IncidentStatus.OPEN, created.getStatus());
        assertEquals(IncidentPriority.HIGH, created.getPriority());

        // Retrieve by ID
        var found = getIncidentUseCase.getIncident(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("Integration test incident", found.get().getTitle());
    }

    @Test
    void shouldCreateAndAssignIncident() {
        // Create
        Incident created = createIncidentUseCase.createIncident(
                "Assignable incident",
                "Will be assigned",
                IncidentPriority.MEDIUM);

        // Assign
        Incident assigned = assignIncidentUseCase.assignIncident(
                created.getId(), assigneeId, teamId);

        assertNotNull(assigned);
        assertEquals(assigneeId, assigned.getAssigneeId());
        assertEquals(teamId, assigned.getTeamId());
        assertEquals(IncidentStatus.OPEN, assigned.getStatus());

        // Verify persisted assignment
        var found = getIncidentUseCase.getIncident(created.getId());
        assertTrue(found.isPresent());
        assertEquals(assigneeId, found.get().getAssigneeId());
        assertEquals(teamId, found.get().getTeamId());
    }

    @Test
    void shouldTransitionIncidentThroughStates() {
        // Create
        Incident created = createIncidentUseCase.createIncident(
                "State machine test",
                "Going through all transitions",
                IncidentPriority.LOW);

        // OPEN → IN_PROGRESS
        Incident inProgress = transitionIncidentUseCase.transitionIncident(
                created.getId(), IncidentStatus.IN_PROGRESS);
        assertEquals(IncidentStatus.IN_PROGRESS, inProgress.getStatus());

        // IN_PROGRESS → RESOLVED
        Incident resolved = transitionIncidentUseCase.transitionIncident(
                created.getId(), IncidentStatus.RESOLVED);
        assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());

        // RESOLVED → CLOSED
        Incident closed = transitionIncidentUseCase.transitionIncident(
                created.getId(), IncidentStatus.CLOSED);
        assertEquals(IncidentStatus.CLOSED, closed.getStatus());

        // Verify final state persisted
        var found = getIncidentUseCase.getIncident(created.getId());
        assertTrue(found.isPresent());
        assertEquals(IncidentStatus.CLOSED, found.get().getStatus());
    }

    @Test
    void shouldReopenResolvedIncident() {
        Incident created = createIncidentUseCase.createIncident(
                "Reopen test",
                "Will be resolved then reopened",
                IncidentPriority.MEDIUM);

        // OPEN → IN_PROGRESS → RESOLVED
        transitionIncidentUseCase.transitionIncident(created.getId(), IncidentStatus.IN_PROGRESS);
        transitionIncidentUseCase.transitionIncident(created.getId(), IncidentStatus.RESOLVED);

        // RESOLVED → OPEN (reopen)
        Incident reopened = transitionIncidentUseCase.transitionIncident(
                created.getId(), IncidentStatus.OPEN);
        assertEquals(IncidentStatus.OPEN, reopened.getStatus());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Incident created = createIncidentUseCase.createIncident(
                "Invalid transition test",
                "Should fail on invalid transition",
                IncidentPriority.MEDIUM);

        // Cannot go directly from OPEN to RESOLVED
        assertThrows(IllegalStateException.class,
                () -> transitionIncidentUseCase.transitionIncident(
                        created.getId(), IncidentStatus.RESOLVED));
    }

    @Test
    void shouldListMultipleIncidents() {
        createIncidentUseCase.createIncident("First", "First incident", IncidentPriority.LOW);
        createIncidentUseCase.createIncident("Second", "Second incident", IncidentPriority.HIGH);
        createIncidentUseCase.createIncident("Third", "Third incident", IncidentPriority.CRITICAL);

        var all = listIncidentsUseCase.listIncidents(
                null, null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertTrue(all.getTotalElements() >= 3);
    }

    @Test
    void shouldThrowWhenAssigningNonExistentIncident() {
        assertThrows(IllegalArgumentException.class,
                () -> assignIncidentUseCase.assignIncident(
                        new IncidentId(UUID.randomUUID()), assigneeId, teamId));
    }

    @Test
    void shouldThrowWhenTransitioningNonExistentIncident() {
        assertThrows(IllegalArgumentException.class,
                () -> transitionIncidentUseCase.transitionIncident(
                        new IncidentId(UUID.randomUUID()), IncidentStatus.IN_PROGRESS));
    }
}

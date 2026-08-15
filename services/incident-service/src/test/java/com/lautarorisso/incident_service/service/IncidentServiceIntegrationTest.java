package com.lautarorisso.incident_service.service;

import com.lautarorisso.incident_service.exception.NotFoundException;
import com.lautarorisso.incident_service.client.UserServiceClient;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentEvent;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link IncidentService}.
 * <p>
 * Loads the full Spring context (test profile: H2 database, disabled Eureka/Config)
 * and mocks external dependencies (Feign UserServiceClient, RabbitMQ).
 */
@SpringBootTest
@ActiveProfiles("test")
class IncidentServiceIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private UUID assigneeId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        outboxEventRepository.deleteAll();
        assigneeId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        when(userServiceClient.findUserById(assigneeId))
                .thenReturn(new UserDto(assigneeId, UUID.randomUUID(), "jdoe", "John Doe",
                        "jdoe@example.com", true, List.of(), Instant.now(), Instant.now()));
        when(userServiceClient.findTeamById(teamId))
                .thenReturn(new TeamDto(teamId, "SRE Team", "Team description",
                        Instant.now(), Instant.now()));
    }

    @Test
    void shouldCreateAndRetrieveIncident() {
        Incident created = incidentService.createIncident(
                "Integration test incident",
                "Testing the full wiring",
                IncidentPriority.HIGH);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Integration test incident", created.getTitle());
        assertEquals(IncidentStatus.OPEN, created.getStatus());
        assertEquals(IncidentPriority.HIGH, created.getPriority());

        var found = incidentService.getIncident(created.getId());
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals("Integration test incident", found.get().getTitle());
    }

    @Test
    void shouldCreateAndAssignIncident() {
        Incident created = incidentService.createIncident(
                "Assignable incident",
                "Will be assigned",
                IncidentPriority.MEDIUM);

        Incident assigned = incidentService.assignIncident(
                created.getId(), assigneeId, teamId);

        assertNotNull(assigned);
        assertEquals(assigneeId, assigned.getAssigneeId());
        assertEquals(teamId, assigned.getTeamId());
        assertEquals(IncidentStatus.OPEN, assigned.getStatus());

        var found = incidentService.getIncident(created.getId());
        assertTrue(found.isPresent());
        assertEquals(assigneeId, found.get().getAssigneeId());
        assertEquals(teamId, found.get().getTeamId());
    }

    @Test
    void shouldTransitionIncidentThroughStates() {
        Incident created = incidentService.createIncident(
                "State machine test",
                "Going through all transitions",
                IncidentPriority.LOW);

        Incident inProgress = incidentService.transitionIncident(
                created.getId(), IncidentStatus.IN_PROGRESS);
        assertEquals(IncidentStatus.IN_PROGRESS, inProgress.getStatus());

        Incident resolved = incidentService.transitionIncident(
                created.getId(), IncidentStatus.RESOLVED);
        assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());

        Incident closed = incidentService.transitionIncident(
                created.getId(), IncidentStatus.CLOSED);
        assertEquals(IncidentStatus.CLOSED, closed.getStatus());

        var found = incidentService.getIncident(created.getId());
        assertTrue(found.isPresent());
        assertEquals(IncidentStatus.CLOSED, found.get().getStatus());
    }

    @Test
    void shouldReopenResolvedIncident() {
        Incident created = incidentService.createIncident(
                "Reopen test",
                "Will be resolved then reopened",
                IncidentPriority.MEDIUM);

        incidentService.transitionIncident(created.getId(), IncidentStatus.IN_PROGRESS);
        incidentService.transitionIncident(created.getId(), IncidentStatus.RESOLVED);

        Incident reopened = incidentService.transitionIncident(
                created.getId(), IncidentStatus.OPEN);
        assertEquals(IncidentStatus.OPEN, reopened.getStatus());
    }

    @Test
    void shouldRejectInvalidTransition() {
        Incident created = incidentService.createIncident(
                "Invalid transition test",
                "Should fail on invalid transition",
                IncidentPriority.MEDIUM);

        assertThrows(IllegalStateException.class,
                () -> incidentService.transitionIncident(
                        created.getId(), IncidentStatus.RESOLVED));
    }

    @Test
    void shouldListMultipleIncidents() {
        incidentService.createIncident("First", "First incident", IncidentPriority.LOW);
        incidentService.createIncident("Second", "Second incident", IncidentPriority.HIGH);
        incidentService.createIncident("Third", "Third incident", IncidentPriority.CRITICAL);

        var all = incidentService.listIncidents(
                null, null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertTrue(all.getTotalElements() >= 3);
    }

    @Test
    void shouldThrowWhenAssigningNonExistentIncident() {
        assertThrows(NotFoundException.class,
                () -> incidentService.assignIncident(
                        UUID.randomUUID(), assigneeId, teamId));
    }

    @Test
    void shouldThrowWhenTransitioningNonExistentIncident() {
        assertThrows(NotFoundException.class,
                () -> incidentService.transitionIncident(
                        UUID.randomUUID(), IncidentStatus.IN_PROGRESS));
    }

    @Test
    void shouldPublishOutboxEventsOnCreate() {
        incidentService.createIncident("Outbox test", "Testing outbox", IncidentPriority.HIGH);

        var unpublished = outboxEventRepository.findByPublishedFalse();
        assertEquals(1, unpublished.size());
        assertEquals(IncidentEvent.INCIDENT_CREATED.name(), unpublished.get(0).getEventType());
    }

    @Test
    void shouldPublishOutboxEventsOnAssign() {
        Incident created = incidentService.createIncident("Assign test", "Desc", IncidentPriority.MEDIUM);

        incidentService.assignIncident(created.getId(), assigneeId, teamId);

        var unpublished = outboxEventRepository.findByPublishedFalse();
        assertEquals(2, unpublished.size());
        assertEquals(IncidentEvent.INCIDENT_CREATED.name(), unpublished.get(0).getEventType());
        assertEquals(IncidentEvent.INCIDENT_ASSIGNED.name(), unpublished.get(1).getEventType());
    }
}

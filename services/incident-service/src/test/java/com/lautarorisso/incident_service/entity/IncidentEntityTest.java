package com.lautarorisso.incident_service.entity;

import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.lautarorisso.incident_service.support.AbstractPostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.lautarorisso.incident_service.enums.IncidentEvent;
import com.lautarorisso.incident_service.enums.IncidentPriority;
import com.lautarorisso.incident_service.enums.IncidentStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link Incident} rich domain model: JPA mapping, basic
 * persistence, the {@code open} factory, assignment and state-machine
 * behavior.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IncidentEntityTest extends AbstractPostgresTestBase {

    @Autowired
    private IncidentRepository incidentRepo;

    @Autowired
    private OutboxEventRepository outboxRepo;

    @BeforeEach
    void setUp() {
        // Clear the V6__seed_data rows so count-based assertions only see the
        // incidents created by each test. The transaction rolls back afterwards,
        // leaving the seed intact for the next run.
        incidentRepo.deleteAll();
    }

    @Test
    void shouldSaveAndFindIncidentEntity() {
        UUID id = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Instant now = Instant.now();

        Incident entity = Incident.builder()
                .id(id)
                .title("Test incident")
                .description("Test description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        incidentRepo.save(entity);
        Optional<Incident> found = incidentRepo.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Test incident", found.get().getTitle());
        assertEquals(IncidentStatus.OPEN, found.get().getStatus());
        assertEquals(IncidentPriority.MEDIUM, found.get().getPriority());
        assertEquals(assigneeId, found.get().getAssigneeId());
        assertEquals(teamId, found.get().getTeamId());
    }

    @Test
    void shouldFindAllIncidentEntities() {
        Incident e1 = Incident.builder()
                .id(UUID.randomUUID())
                .title("A")
                .description("Desc A")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Incident e2 = Incident.builder()
                .id(UUID.randomUUID())
                .title("B")
                .description("Desc B")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.HIGH)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        incidentRepo.save(e1);
        incidentRepo.save(e2);

        assertEquals(2, incidentRepo.findAll().size());
    }

    @Test
    void shouldDeleteIncidentEntity() {
        UUID id = UUID.randomUUID();
        Incident entity = Incident.builder()
                .id(id)
                .title("To delete")
                .description("Will be removed")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        incidentRepo.save(entity);
        assertTrue(incidentRepo.findById(id).isPresent());

        incidentRepo.deleteById(id);
        assertTrue(incidentRepo.findById(id).isEmpty());
    }

    @Test
    void shouldMapAllIncidentFieldsToColumns() {
        UUID id = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Instant now = Instant.now();

        Incident entity = Incident.builder()
                .id(id)
                .title("Full field test")
                .description("Testing all columns are properly mapped")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.CRITICAL)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Incident saved = incidentRepo.save(entity);

        assertEquals("Full field test", saved.getTitle());
        assertEquals("Testing all columns are properly mapped", saved.getDescription());
        assertEquals(IncidentStatus.RESOLVED, saved.getStatus());
        assertEquals(IncidentPriority.CRITICAL, saved.getPriority());
        assertEquals(assigneeId, saved.getAssigneeId());
        assertEquals(teamId, saved.getTeamId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldMapAllStatusAndPriorityValues() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        for (IncidentStatus status : IncidentStatus.values()) {
            for (IncidentPriority priority : IncidentPriority.values()) {
                Incident entity = Incident.builder()
                        .id(id)
                        .title("Test")
                        .description("Test")
                        .status(status)
                        .priority(priority)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                Incident saved = incidentRepo.save(entity);
                Optional<Incident> found = incidentRepo.findById(id);

                assertTrue(found.isPresent());
                assertEquals(status, found.get().getStatus());
                assertEquals(priority, found.get().getPriority());

                incidentRepo.deleteById(id);
            }
        }
    }

    // --- Incident.open ---

    @Test
    void shouldOpenIncidentWithDefaults() {
        Incident incident = Incident.open("  Test incident  ", "  Test description  ", null);

        assertNotNull(incident.getId());
        assertEquals("Test incident", incident.getTitle());
        assertEquals("Test description", incident.getDescription());
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(IncidentPriority.MEDIUM, incident.getPriority());
        assertNotNull(incident.getCreatedAt());
        assertNotNull(incident.getUpdatedAt());
        assertFalse(incident.getUpdatedAt().isBefore(incident.getCreatedAt()));
        assertNull(incident.getAssigneeId());
        assertNull(incident.getTeamId());
    }

    @Test
    void shouldOpenIncidentWithProvidedPriority() {
        Incident incident = Incident.open("  Test  ", null, IncidentPriority.CRITICAL);

        assertEquals("Test", incident.getTitle());
        assertNull(incident.getDescription());
        assertEquals(IncidentPriority.CRITICAL, incident.getPriority());
    }

    @Test
    void shouldRejectBlankOrNullTitleOnOpen() {
        assertThrows(IllegalArgumentException.class,
                () -> Incident.open("", "Desc", null));
        assertThrows(IllegalArgumentException.class,
                () -> Incident.open("   ", "Desc", null));
        assertThrows(IllegalArgumentException.class,
                () -> Incident.open(null, "Desc", null));
    }

    // --- Incident.assignTo ---

    @Test
    void shouldAssignToSetAssigneeTeamAndBumpUpdatedAt() {
        Instant before = Instant.now().minusSeconds(60);
        Incident incident = incident(IncidentStatus.OPEN, before);
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        incident.assignTo(assigneeId, teamId);

        assertEquals(assigneeId, incident.getAssigneeId());
        assertEquals(teamId, incident.getTeamId());
        assertTrue(incident.getUpdatedAt().isAfter(before));
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals("Test", incident.getTitle());
    }

    @Test
    void shouldAssignWithoutTeam() {
        Instant before = Instant.now().minusSeconds(60);
        Incident incident = incident(IncidentStatus.OPEN, before);

        incident.assignTo(UUID.randomUUID(), null);

        assertNotNull(incident.getAssigneeId());
        assertNull(incident.getTeamId());
        assertTrue(incident.getUpdatedAt().isAfter(before));
    }

    @Test
    void shouldRejectNullAssigneeOnAssignTo() {
        Incident incident = incident(IncidentStatus.OPEN, Instant.now());
        UUID teamId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> incident.assignTo(null, teamId));

        assertNull(incident.getAssigneeId());
        assertNull(incident.getTeamId());
    }

    // --- Incident.changeStatus ---

    @Test
    void shouldTransitionThroughValidChainAndBumpUpdatedAt() {
        Instant before = Instant.now().minusSeconds(60);
        Incident incident = incident(IncidentStatus.OPEN, before);

        incident.changeStatus(IncidentStatus.IN_PROGRESS);
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertTrue(incident.getUpdatedAt().isAfter(before));

        Instant inProgressAt = incident.getUpdatedAt();
        incident.changeStatus(IncidentStatus.RESOLVED);
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertTrue(incident.getUpdatedAt().isAfter(inProgressAt));

        Instant resolvedAt = incident.getUpdatedAt();
        incident.changeStatus(IncidentStatus.CLOSED);
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertTrue(incident.getUpdatedAt().isAfter(resolvedAt));
    }

    @Test
    void shouldReopenFromResolvedToOpen() {
        Incident incident = incident(IncidentStatus.RESOLVED, Instant.now());

        incident.changeStatus(IncidentStatus.OPEN);

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
    }

    @Test
    void shouldRejectInvalidTransitions() {
        Incident open = incident(IncidentStatus.OPEN, Instant.now());
        Incident inProgress = incident(IncidentStatus.IN_PROGRESS, Instant.now());
        Incident closed = incident(IncidentStatus.CLOSED, Instant.now());

        assertThrows(IllegalStateException.class,
                () -> open.changeStatus(IncidentStatus.CLOSED));
        assertThrows(IllegalStateException.class,
                () -> open.changeStatus(IncidentStatus.RESOLVED));
        assertThrows(IllegalStateException.class,
                () -> open.changeStatus(IncidentStatus.OPEN));
        assertThrows(IllegalStateException.class,
                () -> inProgress.changeStatus(IncidentStatus.OPEN));
        assertThrows(IllegalStateException.class,
                () -> inProgress.changeStatus(IncidentStatus.CLOSED));
        assertThrows(IllegalStateException.class,
                () -> closed.changeStatus(IncidentStatus.OPEN));
        assertThrows(IllegalStateException.class,
                () -> closed.changeStatus(IncidentStatus.IN_PROGRESS));
        assertThrows(IllegalStateException.class,
                () -> closed.changeStatus(IncidentStatus.RESOLVED));
    }

    @Test
    void shouldRejectNullTargetStatus() {
        Incident incident = incident(IncidentStatus.OPEN, Instant.now());

        assertThrows(IllegalArgumentException.class,
                () -> incident.changeStatus(null));
    }

    // --- Incident.isValidTransition ---

    @Test
    void shouldDetectValidTransition() {
        Incident incident = incident(IncidentStatus.OPEN, Instant.now());

        assertTrue(incident.isValidTransition(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS));
        assertTrue(incident.isValidTransition(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED));
        assertTrue(incident.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.CLOSED));
        assertTrue(incident.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.OPEN));
    }

    @Test
    void shouldDetectInvalidTransition() {
        Incident incident = incident(IncidentStatus.OPEN, Instant.now());

        assertFalse(incident.isValidTransition(IncidentStatus.OPEN, IncidentStatus.CLOSED));
        assertFalse(incident.isValidTransition(IncidentStatus.IN_PROGRESS, IncidentStatus.OPEN));
        assertFalse(incident.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.OPEN));
    }

    @Test
    void shouldSaveAndFindOutboxEvent() {
        UUID id = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setAggregateId(incidentId);
        event.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event.setPayload("{\"incidentId\":\"" + incidentId + "\"}");
        event.setPublished(false);
        event.setCreatedAt(now);

        OutboxEvent saved = outboxRepo.save(event);
        Optional<OutboxEvent> found = outboxRepo.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals(incidentId, found.get().getAggregateId());
        assertEquals(IncidentEvent.INCIDENT_CREATED.name(), found.get().getEventType());
        assertFalse(found.get().isPublished());
    }

    @Test
    void shouldFindUnpublishedOutboxEvents() {
        OutboxEvent e1 = new OutboxEvent();
        e1.setId(UUID.randomUUID());
        e1.setAggregateId(UUID.randomUUID());
        e1.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        e1.setPayload("{}");
        e1.setPublished(false);
        e1.setCreatedAt(Instant.now());

        OutboxEvent e2 = new OutboxEvent();
        e2.setId(UUID.randomUUID());
        e2.setAggregateId(UUID.randomUUID());
        e2.setEventType(IncidentEvent.INCIDENT_ASSIGNED.name());
        e2.setPayload("{}");
        e2.setPublished(true);
        e2.setCreatedAt(Instant.now());

        outboxRepo.save(e1);
        outboxRepo.save(e2);

        assertEquals(1, outboxRepo.findByPublishedFalse().size());
        assertFalse(outboxRepo.findByPublishedFalse().get(0).isPublished());
    }

    @Test
    void shouldMarkOutboxEventAsPublished() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setAggregateId(UUID.randomUUID());
        event.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        event.setPayload("{}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());

        outboxRepo.save(event);
        OutboxEvent saved = outboxRepo.findById(id).orElseThrow();
        saved.setPublished(true);
        outboxRepo.save(saved);

        OutboxEvent updated = outboxRepo.findById(id).orElseThrow();
        assertTrue(updated.isPublished());
    }

    // --- Helpers ---

    private Incident incident(IncidentStatus status, Instant timestamp) {
        return Incident.builder()
                .id(UUID.randomUUID())
                .title("Test")
                .status(status)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}
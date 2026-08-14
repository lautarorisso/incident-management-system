package com.lautarorisso.incident_service.entity;

import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JPA entity mapping and basic persistence operations.
 */
@DataJpaTest
@ActiveProfiles("test")
class IncidentEntityTest {

    @Autowired
    private IncidentRepository incidentRepo;

    @Autowired
    private OutboxEventRepository outboxRepo;

    @Test
    void shouldSaveAndFindIncidentEntity() {
        UUID id = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Instant now = Instant.now();

        Incident entity = new Incident();
        entity.setId(id);
        entity.setTitle("Test incident");
        entity.setDescription("Test description");
        entity.setStatus(IncidentStatus.OPEN);
        entity.setPriority(IncidentPriority.MEDIUM);
        entity.setAssigneeId(assigneeId);
        entity.setTeamId(teamId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

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
        Incident e1 = new Incident();
        e1.setId(UUID.randomUUID());
        e1.setTitle("A");
        e1.setDescription("Desc A");
        e1.setStatus(IncidentStatus.OPEN);
        e1.setPriority(IncidentPriority.LOW);
        e1.setCreatedAt(Instant.now());
        e1.setUpdatedAt(Instant.now());

        Incident e2 = new Incident();
        e2.setId(UUID.randomUUID());
        e2.setTitle("B");
        e2.setDescription("Desc B");
        e2.setStatus(IncidentStatus.IN_PROGRESS);
        e2.setPriority(IncidentPriority.HIGH);
        e2.setCreatedAt(Instant.now());
        e2.setUpdatedAt(Instant.now());

        incidentRepo.save(e1);
        incidentRepo.save(e2);

        assertEquals(2, incidentRepo.findAll().size());
    }

    @Test
    void shouldDeleteIncidentEntity() {
        UUID id = UUID.randomUUID();
        Incident entity = new Incident();
        entity.setId(id);
        entity.setTitle("To delete");
        entity.setDescription("Will be removed");
        entity.setStatus(IncidentStatus.OPEN);
        entity.setPriority(IncidentPriority.LOW);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

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

        Incident entity = new Incident();
        entity.setId(id);
        entity.setTitle("Full field test");
        entity.setDescription("Testing all columns are properly mapped");
        entity.setStatus(IncidentStatus.RESOLVED);
        entity.setPriority(IncidentPriority.CRITICAL);
        entity.setAssigneeId(assigneeId);
        entity.setTeamId(teamId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

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
                Incident entity = new Incident();
                entity.setId(id);
                entity.setTitle("Test");
                entity.setDescription("Test");
                entity.setStatus(status);
                entity.setPriority(priority);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);

                Incident saved = incidentRepo.save(entity);
                Optional<Incident> found = incidentRepo.findById(id);

                assertTrue(found.isPresent());
                assertEquals(status, found.get().getStatus());
                assertEquals(priority, found.get().getPriority());

                incidentRepo.deleteById(id);
            }
        }
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
}

package com.lautarorisso.incident_service.adapter.persistence;

import com.lautarorisso.incident_service.adapter.persistence.entity.IncidentEntity;
import com.lautarorisso.incident_service.adapter.persistence.entity.OutboxEventEntity;
import com.lautarorisso.incident_service.adapter.persistence.repository.IncidentJpaRepository;
import com.lautarorisso.incident_service.adapter.persistence.repository.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JPA entity mapping and Spring Data repository basic operations.
 */
@DataJpaTest
@ActiveProfiles("test")
class IncidentEntityMappingTest {

    @Autowired
    private IncidentJpaRepository incidentRepo;

    @Autowired
    private OutboxEventJpaRepository outboxRepo;

    // --- IncidentEntity Mapping ---

    @Test
    void shouldSaveAndFindIncidentEntity() {
        UUID id = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Instant now = Instant.now();

        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);
        entity.setTitle("Test incident");
        entity.setDescription("Test description");
        entity.setStatus("OPEN");
        entity.setPriority("MEDIUM");
        entity.setAssigneeId(assigneeId);
        entity.setTeamId(teamId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        IncidentEntity saved = incidentRepo.save(entity);
        Optional<IncidentEntity> found = incidentRepo.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Test incident", found.get().getTitle());
        assertEquals("OPEN", found.get().getStatus());
        assertEquals("MEDIUM", found.get().getPriority());
        assertEquals(assigneeId, found.get().getAssigneeId());
        assertEquals(teamId, found.get().getTeamId());
    }

    @Test
    void shouldFindAllIncidentEntities() {
        IncidentEntity e1 = new IncidentEntity();
        e1.setId(UUID.randomUUID());
        e1.setTitle("A");
        e1.setDescription("Desc A");
        e1.setStatus("OPEN");
        e1.setPriority("LOW");
        e1.setCreatedAt(Instant.now());
        e1.setUpdatedAt(Instant.now());
        IncidentEntity e2 = new IncidentEntity();
        e2.setId(UUID.randomUUID());
        e2.setTitle("B");
        e2.setDescription("Desc B");
        e2.setStatus("IN_PROGRESS");
        e2.setPriority("HIGH");
        e2.setCreatedAt(Instant.now());
        e2.setUpdatedAt(Instant.now());

        incidentRepo.save(e1);
        incidentRepo.save(e2);

        List<IncidentEntity> all = incidentRepo.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void shouldDeleteIncidentEntity() {
        UUID id = UUID.randomUUID();
        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);
        entity.setTitle("To delete");
        entity.setDescription("Will be removed");
        entity.setStatus("OPEN");
        entity.setPriority("LOW");
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

        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);
        entity.setTitle("Full field test");
        entity.setDescription("Testing all columns are properly mapped");
        entity.setStatus("RESOLVED");
        entity.setPriority("CRITICAL");
        entity.setAssigneeId(assigneeId);
        entity.setTeamId(teamId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        IncidentEntity saved = incidentRepo.save(entity);

        assertEquals("Full field test", saved.getTitle());
        assertEquals("Testing all columns are properly mapped", saved.getDescription());
        assertEquals("RESOLVED", saved.getStatus());
        assertEquals("CRITICAL", saved.getPriority());
        assertEquals(assigneeId, saved.getAssigneeId());
        assertEquals(teamId, saved.getTeamId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    // --- OutboxEventEntity Mapping ---

    @Test
    void shouldSaveAndFindOutboxEvent() {
        UUID id = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(id);
        event.setAggregateId(incidentId);
        event.setEventType("INCIDENT_CREATED");
        event.setPayload("{\"incidentId\":\"" + incidentId + "\"}");
        event.setPublished(false);
        event.setCreatedAt(now);

        OutboxEventEntity saved = outboxRepo.save(event);
        Optional<OutboxEventEntity> found = outboxRepo.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals(incidentId, found.get().getAggregateId());
        assertEquals("INCIDENT_CREATED", found.get().getEventType());
        assertFalse(found.get().isPublished());
    }

    @Test
    void shouldFindUnpublishedOutboxEvents() {
        OutboxEventEntity e1 = new OutboxEventEntity();
        e1.setId(UUID.randomUUID());
        e1.setAggregateId(UUID.randomUUID());
        e1.setEventType("INCIDENT_CREATED");
        e1.setPayload("{}");
        e1.setPublished(false);
        e1.setCreatedAt(Instant.now());
        OutboxEventEntity e2 = new OutboxEventEntity();
        e2.setId(UUID.randomUUID());
        e2.setAggregateId(UUID.randomUUID());
        e2.setEventType("INCIDENT_ASSIGNED");
        e2.setPayload("{}");
        e2.setPublished(true);
        e2.setCreatedAt(Instant.now());

        outboxRepo.save(e1);
        outboxRepo.save(e2);

        List<OutboxEventEntity> unpublished = outboxRepo.findByPublishedFalse();
        assertEquals(1, unpublished.size());
        assertFalse(unpublished.get(0).isPublished());
    }

    @Test
    void shouldMarkOutboxEventAsPublished() {
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(id);
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("INCIDENT_CREATED");
        event.setPayload("{}");
        event.setPublished(false);
        event.setCreatedAt(Instant.now());

        outboxRepo.save(event);
        OutboxEventEntity saved = outboxRepo.findById(id).orElseThrow();
        saved.setPublished(true);
        outboxRepo.save(saved);

        OutboxEventEntity updated = outboxRepo.findById(id).orElseThrow();
        assertTrue(updated.isPublished());
    }
}

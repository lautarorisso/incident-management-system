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

        IncidentEntity entity = IncidentEntity.builder()
                .id(id)
                .title("Test incident")
                .description("Test description")
                .status("OPEN")
                .priority("MEDIUM")
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

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
        IncidentEntity e1 = IncidentEntity.builder()
                .id(UUID.randomUUID()).title("A").description("Desc A")
                .status("OPEN").priority("LOW").createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        IncidentEntity e2 = IncidentEntity.builder()
                .id(UUID.randomUUID()).title("B").description("Desc B")
                .status("IN_PROGRESS").priority("HIGH").createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        incidentRepo.save(e1);
        incidentRepo.save(e2);

        List<IncidentEntity> all = incidentRepo.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void shouldDeleteIncidentEntity() {
        UUID id = UUID.randomUUID();
        IncidentEntity entity = IncidentEntity.builder()
                .id(id).title("To delete").description("Will be removed")
                .status("OPEN").priority("LOW").createdAt(Instant.now()).updatedAt(Instant.now())
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

        IncidentEntity entity = IncidentEntity.builder()
                .id(id)
                .title("Full field test")
                .description("Testing all columns are properly mapped")
                .status("RESOLVED")
                .priority("CRITICAL")
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

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

        OutboxEventEntity event = OutboxEventEntity.builder()
                .id(id)
                .aggregateId(incidentId)
                .eventType("INCIDENT_CREATED")
                .payload("{\"incidentId\":\"" + incidentId + "\"}")
                .published(false)
                .createdAt(now)
                .build();

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
        OutboxEventEntity e1 = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(UUID.randomUUID())
                .eventType("INCIDENT_CREATED").payload("{}").published(false)
                .createdAt(Instant.now()).build();
        OutboxEventEntity e2 = OutboxEventEntity.builder()
                .id(UUID.randomUUID()).aggregateId(UUID.randomUUID())
                .eventType("INCIDENT_ASSIGNED").payload("{}").published(true)
                .createdAt(Instant.now()).build();

        outboxRepo.save(e1);
        outboxRepo.save(e2);

        List<OutboxEventEntity> unpublished = outboxRepo.findByPublishedFalse();
        assertEquals(1, unpublished.size());
        assertFalse(unpublished.get(0).isPublished());
    }

    @Test
    void shouldMarkOutboxEventAsPublished() {
        UUID id = UUID.randomUUID();
        OutboxEventEntity event = OutboxEventEntity.builder()
                .id(id).aggregateId(UUID.randomUUID())
                .eventType("INCIDENT_CREATED").payload("{}").published(false)
                .createdAt(Instant.now()).build();

        outboxRepo.save(event);
        OutboxEventEntity saved = outboxRepo.findById(id).orElseThrow();
        saved.setPublished(true);
        outboxRepo.save(saved);

        OutboxEventEntity updated = outboxRepo.findById(id).orElseThrow();
        assertTrue(updated.isPublished());
    }
}

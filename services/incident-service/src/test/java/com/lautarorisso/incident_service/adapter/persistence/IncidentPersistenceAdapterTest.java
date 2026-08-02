package com.lautarorisso.incident_service.adapter.persistence;

import com.lautarorisso.incident_service.adapter.persistence.mapper.IncidentEntityMapperImpl;
import com.lautarorisso.incident_service.adapter.persistence.repository.IncidentJpaRepository;
import com.lautarorisso.incident_service.adapter.persistence.repository.OutboxEventJpaRepository;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
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
 * Tests for IncidentPersistenceAdapter — implements both IncidentRepository and IncidentEventPublisher.
 */
@DataJpaTest
@ActiveProfiles("test")
class IncidentPersistenceAdapterTest {

    @Autowired
    private IncidentJpaRepository incidentRepo;

    @Autowired
    private OutboxEventJpaRepository outboxRepo;

    private IncidentPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        var mapper = new IncidentEntityMapperImpl();
        adapter = new IncidentPersistenceAdapter(incidentRepo, outboxRepo, mapper);
    }

    // --- IncidentRepository.save() ---

    @Test
    void shouldSaveIncidentAndPublishOutboxEvent() {
        IncidentId id = new IncidentId(UUID.randomUUID());
        Incident incident = Incident.builder()
                .id(id)
                .title("Save with outbox")
                .description("Testing that save creates both incident and outbox event")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Incident saved = adapter.save(incident);

        // Incident is persisted
        assertNotNull(saved);
        assertEquals(id, saved.getId());
        assertEquals("Save with outbox", saved.getTitle());

        // Outbox event is created
        assertEquals(1, outboxRepo.count());
        var outboxEvents = outboxRepo.findByPublishedFalse();
        assertEquals(1, outboxEvents.size());
        assertEquals(id.getValue(), outboxEvents.get(0).getAggregateId());
    }

    @Test
    void shouldSaveMultipleIncidentsEachCreatingOutboxEvent() {
        IncidentId id1 = new IncidentId(UUID.randomUUID());
        IncidentId id2 = new IncidentId(UUID.randomUUID());

        adapter.save(Incident.builder()
                .id(id1).title("Incident 1").description("Desc")
                .status(IncidentStatus.OPEN).priority(IncidentPriority.LOW)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build());

        adapter.save(Incident.builder()
                .id(id2).title("Incident 2").description("Desc")
                .status(IncidentStatus.OPEN).priority(IncidentPriority.HIGH)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build());

        assertEquals(2, incidentRepo.count());
        assertEquals(2, outboxRepo.count());
    }

    // --- IncidentRepository.findById() ---

    @Test
    void shouldFindIncidentById() {
        IncidentId id = new IncidentId(UUID.randomUUID());
        persistTestIncident(id, "Find me", IncidentStatus.OPEN);

        Optional<Incident> found = adapter.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Find me", found.get().getTitle());
        assertEquals(IncidentStatus.OPEN, found.get().getStatus());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        Optional<Incident> found = adapter.findById(new IncidentId(UUID.randomUUID()));

        assertTrue(found.isEmpty());
    }

    // --- IncidentRepository.findAll() ---

    @Test
    void shouldFindAllIncidents() {
        persistTestIncident(new IncidentId(UUID.randomUUID()), "A", IncidentStatus.OPEN);
        persistTestIncident(new IncidentId(UUID.randomUUID()), "B", IncidentStatus.IN_PROGRESS);

        List<Incident> all = adapter.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoIncidents() {
        List<Incident> all = adapter.findAll();

        assertTrue(all.isEmpty());
    }

    // --- IncidentRepository.deleteById() ---

    @Test
    void shouldDeleteIncidentById() {
        IncidentId id = new IncidentId(UUID.randomUUID());
        persistTestIncident(id, "To delete", IncidentStatus.OPEN);
        assertEquals(1, incidentRepo.count());

        adapter.deleteById(id);

        assertEquals(0, incidentRepo.count());
        assertTrue(adapter.findById(id).isEmpty());
    }

    // --- IncidentEventPublisher.publish() ---

    @Test
    void shouldPublishEventToOutbox() {
        IncidentId id = new IncidentId(UUID.randomUUID());

        adapter.publish(IncidentEvent.INCIDENT_CREATED, id);

        assertEquals(1, outboxRepo.count());
        var event = outboxRepo.findAll().get(0);
        assertEquals(IncidentEvent.INCIDENT_CREATED.name(), event.getEventType());
        assertEquals(id.getValue(), event.getAggregateId());
        assertFalse(event.isPublished());
    }

    @Test
    void shouldPublishMultipleEvents() {
        IncidentId id = new IncidentId(UUID.randomUUID());

        adapter.publish(IncidentEvent.INCIDENT_CREATED, id);
        adapter.publish(IncidentEvent.INCIDENT_ASSIGNED, id);

        assertEquals(2, outboxRepo.count());
    }

    // --- Transactional: save creates incident + event atomically ---

    @Test
    void saveShouldCreateSingleTransactionWithIncidentAndEvent() {
        IncidentId id = new IncidentId(UUID.randomUUID());
        Incident incident = Incident.builder()
                .id(id).title("Transactional save")
                .description("Both incident and event should be in DB")
                .status(IncidentStatus.OPEN).priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        adapter.save(incident);

        // Both persisted in the same transactional context
        assertEquals(1, incidentRepo.count());
        assertEquals(1, outboxRepo.count());
    }

    // --- Helpers ---

    private void persistTestIncident(IncidentId id, String title, IncidentStatus status) {
        var entity = new com.lautarorisso.incident_service.adapter.persistence.entity.IncidentEntity();
        entity.setId(id.getValue());
        entity.setTitle(title);
        entity.setDescription("Description for " + title);
        entity.setStatus(status.name());
        entity.setPriority(IncidentPriority.MEDIUM.name());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        incidentRepo.save(entity);
    }
}

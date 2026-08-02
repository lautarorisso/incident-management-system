package com.lautarorisso.incident_service.adapter.persistence;

import com.lautarorisso.incident_service.adapter.persistence.entity.OutboxEventEntity;
import com.lautarorisso.incident_service.adapter.persistence.mapper.IncidentEntityMapper;
import com.lautarorisso.incident_service.adapter.persistence.repository.IncidentJpaRepository;
import com.lautarorisso.incident_service.adapter.persistence.repository.OutboxEventJpaRepository;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentEvent;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.port.out.IncidentEventPublisher;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter implementing both IncidentRepository and IncidentEventPublisher.
 * <p>
 * Single {@code @Transactional save()} ensures the incident and its outbox event
 * are persisted atomically.
 */
@Component
@RequiredArgsConstructor
public class IncidentPersistenceAdapter implements IncidentRepository, IncidentEventPublisher {

    private final IncidentJpaRepository incidentRepo;
    private final OutboxEventJpaRepository outboxRepo;
    private final IncidentEntityMapper mapper;

    // --- IncidentRepository ---

    @Override
    @Transactional
    public Incident save(Incident incident) {
        var entity = mapper.toEntity(incident);
        var saved = incidentRepo.save(entity);

        // Create outbox event for the domain event
        var outboxEvent = new OutboxEventEntity();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateId(incident.getId().getValue());
        outboxEvent.setEventType(IncidentEvent.INCIDENT_CREATED.name());
        outboxEvent.setPayload("{\"incidentId\":\"" + incident.getId().getValue() + "\"}");
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(Instant.now());
        outboxRepo.save(outboxEvent);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(IncidentId id) {
        return incidentRepo.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findAll() {
        return incidentRepo.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(IncidentId id) {
        incidentRepo.deleteById(id.getValue());
    }

    // --- IncidentEventPublisher ---

    @Override
    @Transactional
    public void publish(IncidentEvent eventType, IncidentId incidentId) {
        var outboxEvent = new OutboxEventEntity();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateId(incidentId.getValue());
        outboxEvent.setEventType(eventType.name());
        outboxEvent.setPayload("{\"incidentId\":\"" + incidentId.getValue() + "\"}");
        outboxEvent.setPublished(false);
        outboxEvent.setCreatedAt(Instant.now());
        outboxRepo.save(outboxEvent);
    }
}

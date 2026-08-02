package com.lautarorisso.incident_service.adapter.persistence;

import com.lautarorisso.incident_service.adapter.persistence.entity.IncidentEntity;
import com.lautarorisso.incident_service.adapter.persistence.mapper.IncidentEntityMapper;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IncidentEntityMapper — MapStruct mapping between domain model and JPA entity.
 */
class IncidentEntityMapperTest {

    private final IncidentEntityMapper mapper = Mappers.getMapper(IncidentEntityMapper.class);

    @Test
    void shouldMapDomainToEntity() {
        IncidentId incidentId = new IncidentId(UUID.randomUUID());
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Instant now = Instant.now();

        Incident domain = Incident.builder()
                .id(incidentId)
                .title("Domain to entity")
                .description("Testing mapping from domain to JPA entity")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.HIGH)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        IncidentEntity entity = mapper.toEntity(domain);

        assertEquals(incidentId.getValue(), entity.getId());
        assertEquals("Domain to entity", entity.getTitle());
        assertEquals("Testing mapping from domain to JPA entity", entity.getDescription());
        assertEquals("IN_PROGRESS", entity.getStatus());
        assertEquals("HIGH", entity.getPriority());
        assertEquals(assigneeId, entity.getAssigneeId());
        assertEquals(teamId, entity.getTeamId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Instant now = Instant.now();

        IncidentEntity entity = new IncidentEntity();
        entity.setId(id);
        entity.setTitle("Entity to domain");
        entity.setDescription("Testing mapping from JPA entity to domain");
        entity.setStatus("RESOLVED");
        entity.setPriority("CRITICAL");
        entity.setAssigneeId(assigneeId);
        entity.setTeamId(teamId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Incident domain = mapper.toDomain(entity);

        assertEquals(id, domain.getId().getValue());
        assertEquals("Entity to domain", domain.getTitle());
        assertEquals("Testing mapping from JPA entity to domain", domain.getDescription());
        assertEquals(IncidentStatus.RESOLVED, domain.getStatus());
        assertEquals(IncidentPriority.CRITICAL, domain.getPriority());
        assertEquals(assigneeId, domain.getAssigneeId());
        assertEquals(teamId, domain.getTeamId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    void shouldMapNullAssigneeAndTeamCorrectly() {
        Incident domain = Incident.builder()
                .id(new IncidentId(UUID.randomUUID()))
                .title("No assignee")
                .description("Testing null assigneeId and teamId")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        IncidentEntity entity = mapper.toEntity(domain);

        assertNull(entity.getAssigneeId());
        assertNull(entity.getTeamId());
    }

    @Test
    void shouldMapAllStatusAndPriorityValues() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        for (IncidentStatus status : IncidentStatus.values()) {
            for (IncidentPriority priority : IncidentPriority.values()) {
                IncidentEntity entity = new IncidentEntity();
                entity.setId(id);
                entity.setTitle("Test");
                entity.setDescription("Test");
                entity.setStatus(status.name());
                entity.setPriority(priority.name());
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);

                Incident domain = mapper.toDomain(entity);

                assertEquals(status, domain.getStatus());
                assertEquals(priority, domain.getPriority());
            }
        }
    }
}

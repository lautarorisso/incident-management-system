package com.lautarorisso.incident_service.repository;

import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IncidentRepository} — verifies the Specification-based
 * filters exercised by {@code IncidentService#listIncidents}, including
 * combined filters.
 */
@DataJpaTest
@ActiveProfiles("test")
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepo;

    @Test
    void shouldFindByStatusAndPriority() {
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, null, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.LOW, null, null);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.HIGH, null, null);

        var result = incidentRepo.findAll(
                spec(IncidentStatus.OPEN, IncidentPriority.HIGH, null, null),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindByStatus() {
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, null, null);
        persistIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.LOW, null, null);

        var result = incidentRepo.findAll(
                spec(IncidentStatus.OPEN, null, null, null),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindByPriority() {
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, null, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.LOW, null, null);

        var result = incidentRepo.findAll(
                spec(null, IncidentPriority.HIGH, null, null),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindByAssigneeIdAndStatus() {
        UUID assigneeId = UUID.randomUUID();
        persistIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM, assigneeId, null);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM, assigneeId, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.LOW, null, null);

        var result = incidentRepo.findAll(
                spec(IncidentStatus.OPEN, null, assigneeId, null),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindByAssigneeId() {
        UUID assigneeId = UUID.randomUUID();
        persistIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM, assigneeId, null);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM, assigneeId, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.LOW, null, null);

        var result = incidentRepo.findAll(
                spec(null, null, assigneeId, null),
                pageable());

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void shouldFindByTeamIdAndStatus() {
        UUID teamId = UUID.randomUUID();
        persistIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM, null, teamId);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM, null, teamId);

        var result = incidentRepo.findAll(
                spec(IncidentStatus.OPEN, null, null, teamId),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindByTeamId() {
        UUID teamId = UUID.randomUUID();
        persistIncident(IncidentStatus.OPEN, IncidentPriority.MEDIUM, null, teamId);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM, null, teamId);

        var result = incidentRepo.findAll(
                spec(null, null, null, teamId),
                pageable());

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void shouldCombineStatusPriorityAndAssigneeFilters() {
        UUID assigneeId = UUID.randomUUID();
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, assigneeId, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.LOW, assigneeId, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, null, null);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.HIGH, assigneeId, null);

        var result = incidentRepo.findAll(
                spec(IncidentStatus.OPEN, IncidentPriority.HIGH, assigneeId, null),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldCombineAllFilters() {
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, assigneeId, teamId);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, assigneeId, null);
        persistIncident(IncidentStatus.OPEN, IncidentPriority.LOW, assigneeId, teamId);

        var result = incidentRepo.findAll(
                spec(IncidentStatus.OPEN, IncidentPriority.HIGH, assigneeId, teamId),
                pageable());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindAllWhenNoFilters() {
        persistIncident(IncidentStatus.OPEN, IncidentPriority.HIGH, null, null);
        persistIncident(IncidentStatus.IN_PROGRESS, IncidentPriority.LOW, null, null);
        persistIncident(IncidentStatus.RESOLVED, IncidentPriority.MEDIUM, null, null);

        var result = incidentRepo.findAll(Specification.where(null), pageable());

        assertEquals(3, result.getTotalElements());
    }

    @Test
    void shouldReturnEmptyPageWhenNoIncidents() {
        var result = incidentRepo.findAll(Specification.where(null), pageable());
        assertEquals(0, result.getTotalElements());
    }

    private static PageRequest pageable() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private static Specification<Incident> spec(IncidentStatus status, IncidentPriority priority,
                                                UUID assigneeId, UUID teamId) {
        Specification<Incident> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (assigneeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assigneeId"), assigneeId));
        }
        if (teamId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("teamId"), teamId));
        }
        return spec;
    }

    private void persistIncident(IncidentStatus status, IncidentPriority priority,
                                  UUID assigneeId, UUID teamId) {
        Incident entity = new Incident();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Test");
        entity.setDescription("Desc");
        entity.setStatus(status);
        entity.setPriority(priority);
        entity.setAssigneeId(assigneeId);
        entity.setTeamId(teamId);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        incidentRepo.save(entity);
    }
}

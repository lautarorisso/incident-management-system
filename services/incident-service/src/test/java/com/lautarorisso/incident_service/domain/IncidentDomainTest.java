package com.lautarorisso.incident_service.domain;

import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import com.lautarorisso.incident_service.domain.port.out.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentDomainTest {

    @Mock
    private IncidentRepository repository;

    // --- Domain Model Tests (T002) ---

    @Test
    void incidentCanBeCreatedWithAllFields() {
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        IncidentId id = new IncidentId(UUID.randomUUID());

        Incident incident = Incident.builder()
                .id(id)
                .title("Database connection failure")
                .description("Production database is not responding")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .build();

        assertEquals(id, incident.getId());
        assertEquals("Database connection failure", incident.getTitle());
        assertEquals("Production database is not responding", incident.getDescription());
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(IncidentPriority.HIGH, incident.getPriority());
        assertEquals(assigneeId, incident.getAssigneeId());
        assertEquals(teamId, incident.getTeamId());
    }

    @Test
    void incidentDefaultsToOpenStatusAndMediumPriority() {
        IncidentId id = new IncidentId(UUID.randomUUID());

        Incident incident = Incident.builder()
                .id(id)
                .title("New incident")
                .description("Something went wrong")
                .build();

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertEquals(IncidentPriority.MEDIUM, incident.getPriority());
    }

    @Test
    void incidentIdWrapsUuid() {
        UUID rawUuid = UUID.randomUUID();
        IncidentId id = new IncidentId(rawUuid);

        assertEquals(rawUuid, id.getValue());
    }

    @Test
    void incidentIdsAreEqualWhenUuidsMatch() {
        UUID rawUuid = UUID.randomUUID();
        IncidentId id1 = new IncidentId(rawUuid);
        IncidentId id2 = new IncidentId(rawUuid);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void incidentIdsAreNotEqualWhenUuidsDiffer() {
        IncidentId id1 = new IncidentId(UUID.randomUUID());
        IncidentId id2 = new IncidentId(UUID.randomUUID());

        assertNotEquals(id1, id2);
    }

    // --- Port Interface Tests (T001) ---

    @Test
    void incidentRepositoryCanSaveAndFindById() {
        IncidentId id = new IncidentId(UUID.randomUUID());
        Incident incident = Incident.builder()
                .id(id)
                .title("Test incident")
                .description("Test description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .build();

        when(repository.save(incident)).thenReturn(incident);
        when(repository.findById(id)).thenReturn(Optional.of(incident));

        Incident saved = repository.save(incident);
        Optional<Incident> found = repository.findById(id);

        assertEquals(incident, saved);
        assertTrue(found.isPresent());
        assertEquals(incident, found.get());
        verify(repository).save(incident);
        verify(repository).findById(id);
    }

    @Test
    void incidentRepositoryFindByIdReturnsEmptyWhenNotFound() {
        IncidentId id = new IncidentId(UUID.randomUUID());

        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<Incident> found = repository.findById(id);

        assertTrue(found.isEmpty());
        verify(repository).findById(id);
    }

    @Test
    void incidentRepositoryFindAllReturnsAllIncidents() {
        IncidentId id1 = new IncidentId(UUID.randomUUID());
        IncidentId id2 = new IncidentId(UUID.randomUUID());
        List<Incident> allIncidents = List.of(
                Incident.builder().id(id1).title("Incident 1").description("Desc 1").build(),
                Incident.builder().id(id2).title("Incident 2").description("Desc 2").build()
        );

        when(repository.findAll()).thenReturn(allIncidents);

        List<Incident> found = repository.findAll();

        assertEquals(2, found.size());
        verify(repository).findAll();
    }

    @Test
    void incidentRepositoryCanDeleteById() {
        IncidentId id = new IncidentId(UUID.randomUUID());

        repository.deleteById(id);

        verify(repository).deleteById(id);
    }

    @Test
    void incidentRepositoryIsInterface() {
        assertTrue(IncidentRepository.class.isInterface());
    }
}

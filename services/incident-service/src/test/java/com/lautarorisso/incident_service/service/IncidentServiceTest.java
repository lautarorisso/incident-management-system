package com.lautarorisso.incident_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.exception.NotFoundException;
import com.lautarorisso.incident_service.client.UserServiceClient;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentEvent;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import com.lautarorisso.incident_service.entity.OutboxEvent;
import com.lautarorisso.incident_service.repository.IncidentRepository;
import com.lautarorisso.incident_service.repository.OutboxEventRepository;
import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private UserServiceClient userServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IncidentStateMachine stateMachine = new IncidentStateMachine();

    private IncidentService incidentService;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxCaptor;

    @Captor
    private ArgumentCaptor<Specification<Incident>> specCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @BeforeEach
    void setUp() {
        // Make save() return the input incident so downstream logic (publishOutbox) works
        lenient().when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
        incidentService = new IncidentService(
                incidentRepository,
                outboxEventRepository,
                userServiceClient,
                stateMachine,
                objectMapper);
    }

    private UserDto activeUser(UUID id) {
        return new UserDto(id, UUID.randomUUID(), "jdoe", "John Doe",
                "jdoe@example.com", true, List.of(), Instant.now(), Instant.now());
    }

    private TeamDto activeTeam(UUID id) {
        return new TeamDto(id, "Backend Team", "Team description",
                Instant.now(), Instant.now());
    }

    private static FeignException feignNotFound() {
        Request request = Request.create(Request.HttpMethod.GET,
                "http://user-service/api/users/x", java.util.Map.of(), null,
                java.nio.charset.StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(request)
                .body(new byte[0])
                .build();
        return FeignException.errorStatus("UserServiceClient#findUserById(UUID)", response);
    }

    // --- createIncident ---

    @Test
    void shouldCreateIncidentWithDefaults() {
        Incident result = incidentService.createIncident("Test incident", "Test description", null);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Test incident", result.getTitle());
        assertEquals("Test description", result.getDescription());
        assertEquals(IncidentStatus.OPEN, result.getStatus());
        assertEquals(IncidentPriority.MEDIUM, result.getPriority());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertNull(result.getAssigneeId());
        assertNull(result.getTeamId());

        verify(incidentRepository).save(any(Incident.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldCreateIncidentWithProvidedPriority() {
        Incident result = incidentService.createIncident("Title", "Desc", IncidentPriority.CRITICAL);

        assertEquals(IncidentPriority.CRITICAL, result.getPriority());
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void shouldTrimTitle() {
        Incident result = incidentService.createIncident("  spaced title  ", "Desc", null);

        assertEquals("spaced title", result.getTitle());
    }

    @Test
    void shouldThrowWhenTitleIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.createIncident("", "Desc", null));
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.createIncident("  ", "Desc", null));
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.createIncident(null, "Desc", null));

        verifyNoInteractions(incidentRepository, outboxEventRepository);
    }

    @Test
    void shouldHandleNullDescription() {
        Incident result = incidentService.createIncident("Title", null, null);

        assertNull(result.getDescription());
    }

    @Test
    void shouldCreateOutboxEventOnCreate() {
        UUID incidentId = UUID.randomUUID();
        Instant now = Instant.now();

        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            return Incident.builder()
                    .id(incidentId)
                    .title(i.getTitle())
                    .description(i.getDescription())
                    .status(i.getStatus())
                    .priority(i.getPriority())
                    .assigneeId(i.getAssigneeId())
                    .teamId(i.getTeamId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        });

        incidentService.createIncident("Test", "Desc", IncidentPriority.HIGH);

        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent outbox = outboxCaptor.getValue();
        assertEquals(IncidentEvent.INCIDENT_CREATED.name(), outbox.getEventType());
        assertEquals(incidentId, outbox.getAggregateId());
        assertFalse(outbox.isPublished());
    }

    // --- assignIncident ---

    @Test
    void shouldAssignIncidentToValidUserAndTeam() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To assign")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));
        when(userServiceClient.findUserById(assigneeId)).thenReturn(activeUser(assigneeId));
        when(userServiceClient.findTeamById(teamId)).thenReturn(activeTeam(teamId));

        Incident result = incidentService.assignIncident(incidentId, assigneeId, teamId);

        assertEquals(assigneeId, result.getAssigneeId());
        assertEquals(teamId, result.getTeamId());
        verify(incidentRepository).save(existing);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowWhenIncidentNotFoundOnAssign() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> incidentService.assignIncident(incidentId, assigneeId, null));
        verifyNoInteractions(userServiceClient, outboxEventRepository);
    }

    @Test
    void shouldThrowWhenAssigneeIdIsNull() {
        UUID incidentId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.assignIncident(incidentId, null, null));
        verifyNoInteractions(incidentRepository, outboxEventRepository);
    }

    @Test
    void shouldThrowWhenUserNotFoundOnAssign() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To assign")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));
        when(userServiceClient.findUserById(assigneeId))
                .thenThrow(feignNotFound());

        assertThrows(FeignException.class,
                () -> incidentService.assignIncident(incidentId, assigneeId, null));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldThrowWhenUserInactiveOnAssign() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To assign")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));
        when(userServiceClient.findUserById(assigneeId))
                .thenReturn(new UserDto(assigneeId, UUID.randomUUID(), "jdoe", "John Doe",
                        "jdoe@example.com", false, List.of(), Instant.now(), Instant.now()));

        assertThrows(NotFoundException.class,
                () -> incidentService.assignIncident(incidentId, assigneeId, null));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldThrowWhenTeamNotFoundOnAssign() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To assign")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));
        when(userServiceClient.findUserById(assigneeId)).thenReturn(activeUser(assigneeId));
        when(userServiceClient.findTeamById(teamId))
                .thenThrow(feignNotFound());

        assertThrows(FeignException.class,
                () -> incidentService.assignIncident(incidentId, assigneeId, teamId));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldAssignWithoutTeam() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To assign")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));
        when(userServiceClient.findUserById(assigneeId)).thenReturn(activeUser(assigneeId));

        Incident result = incidentService.assignIncident(incidentId, assigneeId, null);

        assertEquals(assigneeId, result.getAssigneeId());
        assertNull(result.getTeamId());
        verify(incidentRepository).save(existing);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldCreateOutboxEventOnAssign() {
        UUID incidentId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To assign")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));
        when(userServiceClient.findUserById(assigneeId)).thenReturn(activeUser(assigneeId));

        incidentService.assignIncident(incidentId, assigneeId, null);

        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent outbox = outboxCaptor.getValue();
        assertEquals(IncidentEvent.INCIDENT_ASSIGNED.name(), outbox.getEventType());
    }

    // --- transitionIncident ---

    @Test
    void shouldTransitionIncident() {
        UUID incidentId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To transition")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));

        Incident result = incidentService.transitionIncident(incidentId, IncidentStatus.IN_PROGRESS);

        assertEquals(IncidentStatus.IN_PROGRESS, result.getStatus());
        verify(incidentRepository).save(existing);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowWhenIncidentNotFoundOnTransition() {
        UUID incidentId = UUID.randomUUID();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> incidentService.transitionIncident(incidentId, IncidentStatus.IN_PROGRESS));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldThrowWhenNewStatusIsNull() {
        UUID incidentId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> incidentService.transitionIncident(incidentId, null));
        verifyNoInteractions(incidentRepository, outboxEventRepository);
    }

    @Test
    void shouldThrowOnInvalidTransition() {
        UUID incidentId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To transition")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> incidentService.transitionIncident(incidentId, IncidentStatus.RESOLVED));
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void shouldCreateOutboxEventOnTransition() {
        UUID incidentId = UUID.randomUUID();

        Incident existing = Incident.builder()
                .id(incidentId)
                .title("To transition")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(existing));

        incidentService.transitionIncident(incidentId, IncidentStatus.IN_PROGRESS);

        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent outbox = outboxCaptor.getValue();
        assertEquals(IncidentEvent.INCIDENT_STATUS_CHANGED.name(), outbox.getEventType());
    }

    // --- getIncident ---

    @Test
    void shouldGetIncidentById() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = Incident.builder()
                .id(incidentId)
                .title("Found")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        Optional<Incident> result = incidentService.getIncident(incidentId);

        assertTrue(result.isPresent());
        assertEquals(incidentId, result.get().getId());
        assertEquals("Found", result.get().getTitle());
    }

    @Test
    void shouldReturnEmptyWhenIncidentNotFound() {
        UUID incidentId = UUID.randomUUID();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

        Optional<Incident> result = incidentService.getIncident(incidentId);

        assertTrue(result.isEmpty());
    }

    // --- listIncidents (Specification-based filters) ---

    private List<String> specFields(Specification<Incident> spec) {
        Root<Incident> root = mock(Root.class);
        when(root.get(anyString())).thenAnswer(inv -> mock(Path.class));
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate predicate = mock(Predicate.class);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(cb.and(any(), any())).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);
        verify(root, atLeastOnce()).get(fieldCaptor.capture());
        return fieldCaptor.getAllValues();
    }

    @Test
    void shouldApplyAllFiltersTogether() {
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        incidentService.listIncidents(IncidentStatus.OPEN, IncidentPriority.HIGH,
                assigneeId, teamId, PageRequest.of(0, 10));

        verify(incidentRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertEquals(List.of("status", "priority", "assigneeId", "teamId"),
                specFields(specCaptor.getValue()));
    }

    @Test
    void shouldApplyStatusAndPriorityWithAssignee() {
        UUID assigneeId = UUID.randomUUID();
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        incidentService.listIncidents(IncidentStatus.OPEN, IncidentPriority.HIGH,
                assigneeId, null, PageRequest.of(0, 10));

        verify(incidentRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertEquals(List.of("status", "priority", "assigneeId"),
                specFields(specCaptor.getValue()));
    }

    @Test
    void shouldApplyStatusAndPriorityOnly() {
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        incidentService.listIncidents(IncidentStatus.OPEN, IncidentPriority.HIGH,
                null, null, PageRequest.of(0, 10));

        verify(incidentRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertEquals(List.of("status", "priority"), specFields(specCaptor.getValue()));
    }

    @Test
    void shouldApplyAssigneeOnly() {
        UUID assigneeId = UUID.randomUUID();
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        incidentService.listIncidents(null, null, assigneeId, null, PageRequest.of(0, 10));

        verify(incidentRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertEquals(List.of("assigneeId"), specFields(specCaptor.getValue()));
    }

    @Test
    void shouldUseNoRestrictionsWhenNoFilters() {
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Incident> result = incidentService.listIncidents(null, null, null, null,
                PageRequest.of(0, 10));

        verify(incidentRepository).findAll(nullable(Specification.class), any(Pageable.class));
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldApplyDefaultSortWhenPageableUnsorted() {
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        incidentService.listIncidents(null, null, null, null, PageRequest.of(0, 10));

        verify(incidentRepository).findAll(nullable(Specification.class), pageableCaptor.capture());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageableCaptor.getValue().getSort());
    }

    @Test
    void shouldRespectProvidedSort() {
        Sort sort = Sort.by(Sort.Direction.ASC, "title");
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        incidentService.listIncidents(null, null, null, null, PageRequest.of(0, 10, sort));

        verify(incidentRepository).findAll(nullable(Specification.class), pageableCaptor.capture());
        assertEquals(sort, pageableCaptor.getValue().getSort());
    }

    @Test
    void shouldReturnPageFromRepository() {
        Incident incident = Incident.builder()
                .id(UUID.randomUUID())
                .title("Listed")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(incidentRepository.findAll(nullable(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(incident), pageable, 1));

        Page<Incident> result = incidentService.listIncidents(null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Listed", result.getContent().get(0).getTitle());
    }
}

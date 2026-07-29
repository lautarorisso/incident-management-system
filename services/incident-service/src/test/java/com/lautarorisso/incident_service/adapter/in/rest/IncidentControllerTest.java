package com.lautarorisso.incident_service.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.adapter.in.rest.controller.IncidentController;
import com.lautarorisso.incident_service.adapter.in.rest.dto.AssignIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.dto.CreateIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.dto.IncidentListItem;
import com.lautarorisso.incident_service.adapter.in.rest.dto.IncidentResponse;
import com.lautarorisso.incident_service.adapter.in.rest.dto.TransitionIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.mapper.IncidentRestMapper;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import com.lautarorisso.incident_service.domain.port.in.AssignIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.CreateIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.GetIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.ListIncidentsUseCase;
import com.lautarorisso.incident_service.domain.port.in.TransitionIncidentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC tests for IncidentController.
 * <p>
 * Mocks all use case dependencies and the mapper to test HTTP
 * request/response handling, validation, and error scenarios.
 */
@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateIncidentUseCase createIncidentUseCase;

    @MockitoBean
    private AssignIncidentUseCase assignIncidentUseCase;

    @MockitoBean
    private TransitionIncidentUseCase transitionIncidentUseCase;

    @MockitoBean
    private GetIncidentUseCase getIncidentUseCase;

    @MockitoBean
    private ListIncidentsUseCase listIncidentsUseCase;

    @MockitoBean
    private IncidentRestMapper mapper;

    private final UUID incidentUuid = UUID.randomUUID();
    private final Instant now = Instant.now();

    // --- POST /api/incidents ---

    @Test
    void shouldCreateIncident() throws Exception {
        var request = CreateIncidentRequest.builder()
                .title("Test incident")
                .description("Test description")
                .priority("HIGH")
                .build();

        var domain = Incident.builder()
                .id(new IncidentId(incidentUuid))
                .title("Test incident")
                .description("Test description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var response = IncidentResponse.builder()
                .id(incidentUuid)
                .title("Test incident")
                .description("Test description")
                .status("OPEN")
                .priority("HIGH")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(createIncidentUseCase.createIncident("Test incident", "Test description", IncidentPriority.HIGH))
                .thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(incidentUuid.toString()))
                .andExpect(jsonPath("$.title").value("Test incident"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void shouldReturn400WhenCreateTitleIsBlank() throws Exception {
        var request = CreateIncidentRequest.builder()
                .title("")
                .description("Some description")
                .build();

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCreateTitleIsNull() throws Exception {
        var request = CreateIncidentRequest.builder()
                .description("Some description")
                .build();

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/incidents/{id}/assign ---

    @Test
    void shouldAssignIncident() throws Exception {
        UUID assigneeId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        var request = AssignIncidentRequest.builder()
                .assigneeId(assigneeId)
                .teamId(teamId)
                .build();

        var domain = Incident.builder()
                .id(new IncidentId(incidentUuid))
                .title("Assigned incident")
                .description("Now assigned")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.MEDIUM)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var response = IncidentResponse.builder()
                .id(incidentUuid)
                .title("Assigned incident")
                .description("Now assigned")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(assignIncidentUseCase.assignIncident(new IncidentId(incidentUuid), assigneeId, teamId))
                .thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(put("/api/incidents/{id}/assign", incidentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentUuid.toString()))
                .andExpect(jsonPath("$.assigneeId").value(assigneeId.toString()))
                .andExpect(jsonPath("$.teamId").value(teamId.toString()));
    }

    @Test
    void shouldReturn400WhenAssignAssigneeIdIsNull() throws Exception {
        var request = AssignIncidentRequest.builder()
                .teamId(UUID.randomUUID())
                .build();

        mockMvc.perform(put("/api/incidents/{id}/assign", incidentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/incidents/{id}/transition ---

    @Test
    void shouldTransitionIncident() throws Exception {
        var request = TransitionIncidentRequest.builder()
                .newStatus("IN_PROGRESS")
                .build();

        var domain = Incident.builder()
                .id(new IncidentId(incidentUuid))
                .title("Transitioned incident")
                .description("In progress now")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var response = IncidentResponse.builder()
                .id(incidentUuid)
                .title("Transitioned incident")
                .description("In progress now")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(transitionIncidentUseCase.transitionIncident(new IncidentId(incidentUuid), IncidentStatus.IN_PROGRESS))
                .thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(put("/api/incidents/{id}/transition", incidentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldReturn400WhenTransitionStatusIsBlank() throws Exception {
        var request = TransitionIncidentRequest.builder()
                .newStatus("")
                .build();

        mockMvc.perform(put("/api/incidents/{id}/transition", incidentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/incidents/{id} ---

    @Test
    void shouldGetIncidentById() throws Exception {
        var domain = Incident.builder()
                .id(new IncidentId(incidentUuid))
                .title("Found incident")
                .description("Found description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var response = IncidentResponse.builder()
                .id(incidentUuid)
                .title("Found incident")
                .description("Found description")
                .status("OPEN")
                .priority("CRITICAL")
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(getIncidentUseCase.getIncident(new IncidentId(incidentUuid)))
                .thenReturn(Optional.of(domain));
        when(mapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/{id}", incidentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentUuid.toString()))
                .andExpect(jsonPath("$.title").value("Found incident"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();

        when(getIncidentUseCase.getIncident(new IncidentId(missingId)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/incidents/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/incidents ---

    @Test
    void shouldListAllIncidents() throws Exception {
        var domain1 = Incident.builder()
                .id(new IncidentId(UUID.randomUUID()))
                .title("Incident A")
                .description("Desc A")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var domain2 = Incident.builder()
                .id(new IncidentId(UUID.randomUUID()))
                .title("Incident B")
                .description("Desc B")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var item1 = IncidentListItem.builder()
                .id(domain1.getId().getValue())
                .title("Incident A")
                .status("OPEN")
                .priority("LOW")
                .createdAt(now)
                .build();

        var item2 = IncidentListItem.builder()
                .id(domain2.getId().getValue())
                .title("Incident B")
                .status("RESOLVED")
                .priority("HIGH")
                .createdAt(now)
                .build();

        when(listIncidentsUseCase.listIncidents()).thenReturn(List.of(domain1, domain2));
        when(mapper.toListItem(domain1)).thenReturn(item1);
        when(mapper.toListItem(domain2)).thenReturn(item2);

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Incident A"))
                .andExpect(jsonPath("$[1].title").value("Incident B"));
    }

    @Test
    void shouldReturnEmptyListWhenNoIncidents() throws Exception {
        when(listIncidentsUseCase.listIncidents()).thenReturn(List.of());

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

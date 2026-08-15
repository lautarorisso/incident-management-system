package com.lautarorisso.incident_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.incident_service.dto.AssignIncidentRequest;
import com.lautarorisso.incident_service.dto.CreateIncidentRequest;
import com.lautarorisso.incident_service.dto.TransitionIncidentRequest;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import com.lautarorisso.incident_service.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC tests for {@link IncidentController}.
 * <p>
 * Mocks {@link IncidentService} to test HTTP request/response handling,
 * validation, and error scenarios with manual DTO mapping.
 */
@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private com.lautarorisso.incident_service.client.UserServiceClient userServiceClient;

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

        Incident domain = Incident.builder()
                .id(incidentUuid)
                .title("Test incident")
                .description("Test description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.HIGH)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(incidentService.createIncident("Test incident", "Test description", IncidentPriority.HIGH))
                .thenReturn(domain);

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

    @Test
    void shouldReturn400WhenCreatePriorityIsInvalid() throws Exception {
        var request = CreateIncidentRequest.builder()
                .title("Test")
                .priority("INVALID")
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

        Incident domain = Incident.builder()
                .id(incidentUuid)
                .title("Assigned incident")
                .description("Now assigned")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.MEDIUM)
                .assigneeId(assigneeId)
                .teamId(teamId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(incidentService.assignIncident(incidentUuid, assigneeId, teamId))
                .thenReturn(domain);

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

        Incident domain = Incident.builder()
                .id(incidentUuid)
                .title("Transitioned incident")
                .description("In progress now")
                .status(IncidentStatus.IN_PROGRESS)
                .priority(IncidentPriority.MEDIUM)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(incidentService.transitionIncident(incidentUuid, IncidentStatus.IN_PROGRESS))
                .thenReturn(domain);

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

    @Test
    void shouldReturn400WhenTransitionStatusIsInvalid() throws Exception {
        var request = TransitionIncidentRequest.builder()
                .newStatus("INVALID")
                .build();

        mockMvc.perform(put("/api/incidents/{id}/transition", incidentUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/incidents/{id} ---

    @Test
    void shouldGetIncidentById() throws Exception {
        Incident domain = Incident.builder()
                .id(incidentUuid)
                .title("Found incident")
                .description("Found description")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.CRITICAL)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(incidentService.getIncident(incidentUuid))
                .thenReturn(Optional.of(domain));

        mockMvc.perform(get("/api/incidents/{id}", incidentUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentUuid.toString()))
                .andExpect(jsonPath("$.title").value("Found incident"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();

        when(incidentService.getIncident(missingId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/incidents/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenNotFoundExceptionThrown() throws Exception {
        when(incidentService.getIncident(incidentUuid))
                .thenThrow(new com.lautarorisso.incident_service.exception.NotFoundException("Incident not found: " + incidentUuid));

        mockMvc.perform(get("/api/incidents/{id}", incidentUuid))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/incidents ---

    @Test
    void shouldListAllIncidents() throws Exception {
        Incident domain1 = Incident.builder()
                .id(UUID.randomUUID())
                .title("Incident A")
                .description("Desc A")
                .status(IncidentStatus.OPEN)
                .priority(IncidentPriority.LOW)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Incident domain2 = Incident.builder()
                .id(UUID.randomUUID())
                .title("Incident B")
                .description("Desc B")
                .status(IncidentStatus.RESOLVED)
                .priority(IncidentPriority.HIGH)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(incidentService.listIncidents(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(domain1, domain2)));

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Incident A"))
                .andExpect(jsonPath("$.content[1].title").value("Incident B"));
    }

    @Test
    void shouldReturnEmptyPageWhenNoIncidents() throws Exception {
        when(incidentService.listIncidents(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}

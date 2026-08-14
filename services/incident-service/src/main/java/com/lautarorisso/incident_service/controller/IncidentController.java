package com.lautarorisso.incident_service.controller;

import com.lautarorisso.incident_service.dto.AssignIncidentRequest;
import com.lautarorisso.incident_service.dto.CreateIncidentRequest;
import com.lautarorisso.incident_service.dto.IncidentResponse;
import com.lautarorisso.incident_service.dto.TransitionIncidentRequest;
import com.lautarorisso.incident_service.entity.Incident;
import com.lautarorisso.incident_service.entity.IncidentPriority;
import com.lautarorisso.incident_service.entity.IncidentStatus;
import com.lautarorisso.incident_service.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Incident CRUD and state machine operations.
 * <p>
 * Maps HTTP requests to service method invocations using manual DTO
 * mapping (no MapStruct).
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident management endpoints")
public class IncidentController {

    private final IncidentService incidentService;

    // --- POST /api/incidents ---

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new incident", description = "Creates an incident with the given title, description, and priority")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incident created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {

        IncidentPriority priority = parseEnum(IncidentPriority.class, request.getPriority(), "priority");

        Incident incident = incidentService.createIncident(
                request.getTitle(),
                request.getDescription(),
                priority);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(incident));
    }

    // --- PUT /api/incidents/{id}/assign ---

    @PutMapping(value = "/{id}/assign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Assign an incident", description = "Assigns an incident to a user and optionally a team")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or assignment failed"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponse> assignIncident(
            @Parameter(description = "Incident UUID") @PathVariable("id") UUID id,
            @Valid @RequestBody AssignIncidentRequest request) {

        Incident incident = incidentService.assignIncident(
                id,
                request.getAssigneeId(),
                request.getTeamId());

        return ResponseEntity.ok(toResponse(incident));
    }

    // --- PUT /api/incidents/{id}/transition ---

    @PutMapping(value = "/{id}/transition", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Transition incident status", description = "Transitions an incident to a new status (OPEN → IN_PROGRESS → RESOLVED → CLOSED, with reopen)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident transitioned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transition"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponse> transitionIncident(
            @Parameter(description = "Incident UUID") @PathVariable("id") UUID id,
            @Valid @RequestBody TransitionIncidentRequest request) {

        IncidentStatus newStatus = parseEnum(IncidentStatus.class, request.getNewStatus(), "status");

        Incident incident = incidentService.transitionIncident(id, newStatus);

        return ResponseEntity.ok(toResponse(incident));
    }

    // --- GET /api/incidents/{id} ---

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get incident by ID", description = "Returns full incident details for the given UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident found"),
            @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    public ResponseEntity<IncidentResponse> getIncident(
            @Parameter(description = "Incident UUID") @PathVariable("id") UUID id) {

        return incidentService.getIncident(id)
                .map(incident -> ResponseEntity.ok(toResponse(incident)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- GET /api/incidents ---

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List incidents with filters and pagination",
            description = "Returns a paginated list of incidents, optionally filtered by status, priority, assignee, or team, ordered by createdAt descending")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of incidents")
    })
    public ResponseEntity<Page<IncidentResponse>> listIncidents(
            @Parameter(description = "Filter by status (e.g. OPEN, IN_PROGRESS, RESOLVED, CLOSED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority (e.g. LOW, MEDIUM, HIGH, CRITICAL)")
            @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by assignee UUID")
            @RequestParam(required = false) UUID assigneeId,
            @Parameter(description = "Filter by team UUID")
            @RequestParam(required = false) UUID teamId,
            @ParameterObject Pageable pageable) {

        IncidentStatus statusEnum = parseEnum(IncidentStatus.class, status, "status");
        IncidentPriority priorityEnum = parseEnum(IncidentPriority.class, priority, "priority");

        Page<Incident> incidents = incidentService.listIncidents(
                statusEnum, priorityEnum, assigneeId, teamId, pageable);

        List<IncidentResponse> responses = incidents.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Page<IncidentResponse> responsePage = new PageImpl<>(responses,
                pageable, incidents.getTotalElements());

        return ResponseEntity.ok(responsePage);
    }

    // --- Manual mapping helpers (no MapStruct) ---

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid " + label + ": " + value);
        }
    }

    private IncidentResponse toResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .status(incident.getStatus() != null ? incident.getStatus().name() : null)
                .priority(incident.getPriority() != null ? incident.getPriority().name() : null)
                .assigneeId(incident.getAssigneeId())
                .teamId(incident.getTeamId())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}

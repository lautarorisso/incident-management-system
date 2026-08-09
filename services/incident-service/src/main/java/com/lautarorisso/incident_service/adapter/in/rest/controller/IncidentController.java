package com.lautarorisso.incident_service.adapter.in.rest.controller;

import com.lautarorisso.incident_service.adapter.in.rest.dto.AssignIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.dto.CreateIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.dto.IncidentResponse;
import com.lautarorisso.incident_service.adapter.in.rest.dto.TransitionIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.mapper.IncidentRestMapper;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import com.lautarorisso.incident_service.domain.port.in.AssignIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.CreateIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.GetIncidentUseCase;
import com.lautarorisso.incident_service.domain.port.in.ListIncidentsUseCase;
import com.lautarorisso.incident_service.domain.port.in.TransitionIncidentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
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

import java.util.UUID;

/**
 * REST controller for Incident CRUD and state machine operations.
 * <p>
 * Maps HTTP requests to use case invocations via the hexagonal architecture.
 */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident management endpoints")
public class IncidentController {

    private final CreateIncidentUseCase createIncidentUseCase;
    private final AssignIncidentUseCase assignIncidentUseCase;
    private final TransitionIncidentUseCase transitionIncidentUseCase;
    private final GetIncidentUseCase getIncidentUseCase;
    private final ListIncidentsUseCase listIncidentsUseCase;
    private final IncidentRestMapper mapper;

    // --- POST /api/incidents ---

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new incident", description = "Creates an incident with the given title, description, and priority")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incident created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {

        IncidentPriority priority = request.getPriority() != null
                ? IncidentPriority.valueOf(request.getPriority().toUpperCase())
                : null;

        var incident = createIncidentUseCase.createIncident(
                request.getTitle(),
                request.getDescription(),
                priority);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(incident));
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

        var incident = assignIncidentUseCase.assignIncident(
                new IncidentId(id),
                request.getAssigneeId(),
                request.getTeamId());

        return ResponseEntity.ok(mapper.toResponse(incident));
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

        var newStatus = IncidentStatus.valueOf(request.getNewStatus().toUpperCase());

        var incident = transitionIncidentUseCase.transitionIncident(
                new IncidentId(id),
                newStatus);

        return ResponseEntity.ok(mapper.toResponse(incident));
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

        return getIncidentUseCase.getIncident(new IncidentId(id))
                .map(incident -> ResponseEntity.ok(mapper.toResponse(incident)))
                .orElse(ResponseEntity.notFound().build());
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

        var incidents = listIncidentsUseCase.listIncidents(status, priority, assigneeId, teamId, pageable);
        return ResponseEntity.ok(incidents.map(mapper::toResponse));
    }
}

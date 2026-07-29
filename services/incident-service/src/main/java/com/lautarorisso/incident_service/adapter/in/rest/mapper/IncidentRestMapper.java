package com.lautarorisso.incident_service.adapter.in.rest.mapper;

import com.lautarorisso.incident_service.adapter.in.rest.dto.CreateIncidentRequest;
import com.lautarorisso.incident_service.adapter.in.rest.dto.IncidentListItem;
import com.lautarorisso.incident_service.adapter.in.rest.dto.IncidentResponse;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * MapStruct mapper between Incident domain model and REST DTOs.
 * <p>
 * Uses componentModel = "spring" for dependency injection into controllers.
 */
@Mapper(componentModel = "spring")
public interface IncidentRestMapper {

    // --- CreateIncidentRequest → Incident ---

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assigneeId", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToPriority")
    Incident toDomain(CreateIncidentRequest request);

    // --- Incident → IncidentResponse ---

    @Mapping(target = "id", source = "id", qualifiedByName = "incidentIdToUuid")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "priorityToString")
    IncidentResponse toResponse(Incident incident);

    // --- Incident → IncidentListItem ---

    @Mapping(target = "id", source = "id", qualifiedByName = "incidentIdToUuid")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "priorityToString")
    IncidentListItem toListItem(Incident incident);

    // --- Status mappings ---

    @Named("statusToString")
    default String statusToString(IncidentStatus status) {
        return status != null ? status.name() : null;
    }

    // --- Priority mappings ---

    @Named("priorityToString")
    default String priorityToString(IncidentPriority priority) {
        return priority != null ? priority.name() : null;
    }

    @Named("stringToPriority")
    default IncidentPriority stringToPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return IncidentPriority.MEDIUM;
        }
        return IncidentPriority.valueOf(priority.toUpperCase());
    }

    // --- IncidentId <-> UUID mappings ---

    @Named("incidentIdToUuid")
    default UUID incidentIdToUuid(IncidentId incidentId) {
        return incidentId != null ? incidentId.getValue() : null;
    }
}

package com.lautarorisso.incident_service.adapter.persistence.mapper;

import com.lautarorisso.incident_service.adapter.persistence.entity.IncidentEntity;
import com.lautarorisso.incident_service.domain.model.Incident;
import com.lautarorisso.incident_service.domain.model.IncidentId;
import com.lautarorisso.incident_service.domain.model.IncidentPriority;
import com.lautarorisso.incident_service.domain.model.IncidentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * MapStruct mapper between Incident domain model and IncidentEntity JPA entity.
 * <p>
 * Uses componentModel = "spring" for dependency injection into adapters.
 */
@Mapper(componentModel = "spring")
public interface IncidentEntityMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "incidentIdToUuid")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "priorityToString")
    IncidentEntity toEntity(Incident domain);

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToIncidentId")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToPriority")
    Incident toDomain(IncidentEntity entity);

    // --- Status mappings ---

    @Named("statusToString")
    default String statusToString(IncidentStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatus")
    default IncidentStatus stringToStatus(String status) {
        return status != null ? IncidentStatus.valueOf(status) : null;
    }

    // --- Priority mappings ---

    @Named("priorityToString")
    default String priorityToString(IncidentPriority priority) {
        return priority != null ? priority.name() : null;
    }

    @Named("stringToPriority")
    default IncidentPriority stringToPriority(String priority) {
        return priority != null ? IncidentPriority.valueOf(priority) : null;
    }

    // --- IncidentId <-> UUID mappings ---

    @Named("incidentIdToUuid")
    default UUID incidentIdToUuid(IncidentId incidentId) {
        return incidentId != null ? incidentId.getValue() : null;
    }

    @Named("uuidToIncidentId")
    default IncidentId uuidToIncidentId(UUID uuid) {
        return uuid != null ? new IncidentId(uuid) : null;
    }
}

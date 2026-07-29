package com.lautarorisso.notification_service.adapter.out.persistence.mapper;

import com.lautarorisso.notification_service.adapter.out.persistence.entity.ProcessedEventEntity;
import com.lautarorisso.notification_service.domain.model.ProcessedEvent;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between ProcessedEvent domain model and ProcessedEventEntity.
 */
@Mapper(componentModel = "spring")
public interface ProcessedEventEntityMapper {

    ProcessedEventEntity toEntity(ProcessedEvent domain);

    ProcessedEvent toDomain(ProcessedEventEntity entity);
}

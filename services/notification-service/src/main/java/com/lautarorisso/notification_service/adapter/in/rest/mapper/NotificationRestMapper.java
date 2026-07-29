package com.lautarorisso.notification_service.adapter.in.rest.mapper;

import com.lautarorisso.notification_service.adapter.in.rest.dto.NotificationListItem;
import com.lautarorisso.notification_service.adapter.in.rest.dto.NotificationResponse;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper between Notification domain model and REST DTOs.
 */
@Mapper(componentModel = "spring")
public interface NotificationRestMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "notificationIdToUuid")
    @Mapping(target = "type", source = "type", qualifiedByName = "typeToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    NotificationResponse toResponse(Notification domain);

    @Mapping(target = "id", source = "id", qualifiedByName = "notificationIdToUuid")
    @Mapping(target = "type", source = "type", qualifiedByName = "typeToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    NotificationListItem toListItem(Notification domain);

    List<NotificationResponse> toResponseList(List<Notification> domains);

    List<NotificationListItem> toListItemList(List<Notification> domains);

    // --- NotificationId <-> UUID mappings ---

    @Named("notificationIdToUuid")
    default UUID notificationIdToUuid(NotificationId notificationId) {
        return notificationId != null ? notificationId.getValue() : null;
    }

    // --- Type mappings ---

    @Named("typeToString")
    default String typeToString(NotificationType type) {
        return type != null ? type.name() : null;
    }

    // --- Status mappings ---

    @Named("statusToString")
    default String statusToString(NotificationStatus status) {
        return status != null ? status.name() : null;
    }
}

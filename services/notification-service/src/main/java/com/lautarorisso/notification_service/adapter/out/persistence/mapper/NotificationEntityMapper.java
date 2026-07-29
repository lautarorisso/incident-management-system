package com.lautarorisso.notification_service.adapter.out.persistence.mapper;

import com.lautarorisso.notification_service.adapter.out.persistence.entity.NotificationEntity;
import com.lautarorisso.notification_service.domain.model.Notification;
import com.lautarorisso.notification_service.domain.model.NotificationId;
import com.lautarorisso.notification_service.domain.model.NotificationStatus;
import com.lautarorisso.notification_service.domain.model.NotificationType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * MapStruct mapper between Notification domain model and NotificationEntity.
 */
@Mapper(componentModel = "spring")
public interface NotificationEntityMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "notificationIdToUuid")
    @Mapping(target = "type", source = "type", qualifiedByName = "typeToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    NotificationEntity toEntity(Notification domain);

    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToNotificationId")
    @Mapping(target = "type", source = "type", qualifiedByName = "stringToType")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    Notification toDomain(NotificationEntity entity);

    // --- NotificationId <-> UUID mappings ---

    @Named("notificationIdToUuid")
    default UUID notificationIdToUuid(NotificationId notificationId) {
        return notificationId != null ? notificationId.getValue() : null;
    }

    @Named("uuidToNotificationId")
    default NotificationId uuidToNotificationId(UUID uuid) {
        return uuid != null ? new NotificationId(uuid) : null;
    }

    // --- Type mappings ---

    @Named("typeToString")
    default String typeToString(NotificationType type) {
        return type != null ? type.name() : null;
    }

    @Named("stringToType")
    default NotificationType stringToType(String type) {
        return type != null ? NotificationType.valueOf(type) : null;
    }

    // --- Status mappings ---

    @Named("statusToString")
    default String statusToString(NotificationStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("stringToStatus")
    default NotificationStatus stringToStatus(String status) {
        return status != null ? NotificationStatus.valueOf(status) : null;
    }
}

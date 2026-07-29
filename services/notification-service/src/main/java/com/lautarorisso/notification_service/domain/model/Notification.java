package com.lautarorisso.notification_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain model representing a Notification.
 * <p>
 * A Notification is created in response to an incident event and targeted
 * at a specific user. It has a type, title, message, and read status.
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private NotificationId id;
    private NotificationType type;
    private UUID userId;
    private UUID incidentId;
    private String title;
    private String message;

    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    private Instant createdAt;

    /**
     * Returns a new Notification with the given status, leaving this instance unchanged.
     */
    public Notification withStatus(NotificationStatus newStatus) {
        return Notification.builder()
                .id(this.id)
                .type(this.type)
                .userId(this.userId)
                .incidentId(this.incidentId)
                .title(this.title)
                .message(this.message)
                .status(newStatus)
                .createdAt(this.createdAt)
                .build();
    }
}

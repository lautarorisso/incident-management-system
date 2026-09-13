package com.lautarorisso.notification_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;
import com.lautarorisso.notification_service.enums.NotificationStatus;
import com.lautarorisso.notification_service.enums.NotificationType;

/**
 * MongoDB document mapping the notifications collection.
 * <p>
 * In the layered architecture this document serves as both the persistence
 * model and the domain model — no separate NotificationId value object.
 */
@Document(collection = "notifications")
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @EqualsAndHashCode.Include
    @Id
    private UUID id;

    private NotificationType type;

    private UUID userId;

    private UUID incidentId;

    private String title;

    private String message;

    /**
     * The outbox event id that produced this notification. Nullable: legacy
     * events published before the eventId stamp existed carry no id. Kept out
     * of every response DTO — it is a dedup key, not API surface.
     */
    private String eventId;

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
                .eventId(this.eventId)
                .status(newStatus)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * Returns a new Notification marked as SENT.
     */
    public Notification markAsSent() {
        return withStatus(NotificationStatus.SENT);
    }

    /**
     * Returns a new Notification marked as FAILED.
     */
    public Notification markAsFailed() {
        return withStatus(NotificationStatus.FAILED);
    }
}

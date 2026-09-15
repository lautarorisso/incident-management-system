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
import com.lautarorisso.notification_service.enums.NotificationDeliveryStatus;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.enums.NotificationType;

/**
 * MongoDB document mapping the notifications collection.
 * <p>
 * In the layered architecture this document serves as both the persistence
 * model and the domain model — no separate NotificationId value object.
 * <p>
 * Delivery and read state are modeled as two independent enum fields
 * ({@link NotificationDeliveryStatus}, {@link NotificationReadStatus}).
 * Legacy documents carrying only {@code status} are covered by the
 * {@link Builder @Builder} defaults and are not migrated.
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

    /**
     * Recipient email denormalized from the incident event payload
     * ({@code assigneeEmail}). Nullable: legacy events and status events
     * without an assignee carry no email; {@code EmailNotificationSender}
     * falls back to the placeholder address in that case. Kept out of every
     * response DTO — internal delivery data, not API surface.
     */
    private String recipientEmail;

    @Builder.Default
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    @Builder.Default
    private NotificationReadStatus readStatus = NotificationReadStatus.UNREAD;

    private Instant createdAt;

    /**
     * Returns a copy of this Notification with the given delivery/read status,
     * leaving this instance unchanged (immutability style).
     */
    private Notification copyWith(NotificationDeliveryStatus newDeliveryStatus,
                                  NotificationReadStatus newReadStatus) {
        return Notification.builder()
                .id(this.id)
                .type(this.type)
                .userId(this.userId)
                .incidentId(this.incidentId)
                .title(this.title)
                .message(this.message)
                .eventId(this.eventId)
                .recipientEmail(this.recipientEmail)
                .deliveryStatus(newDeliveryStatus)
                .readStatus(newReadStatus)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * Returns a new Notification marked as delivered (SENT), leaving this
     * instance unchanged. The read status is preserved.
     */
    public Notification markDelivered() {
        return copyWith(NotificationDeliveryStatus.SENT, this.readStatus);
    }

    /**
     * Returns a new Notification marked as FAILED, leaving this instance
     * unchanged. The read status is preserved.
     */
    public Notification markFailed() {
        return copyWith(NotificationDeliveryStatus.FAILED, this.readStatus);
    }

    /**
     * Returns a new Notification with the given read status, leaving this
     * instance unchanged. The delivery status is preserved.
     */
    public Notification withReadStatus(NotificationReadStatus newReadStatus) {
        return copyWith(this.deliveryStatus, newReadStatus);
    }
}

package com.lautarorisso.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the notifications table.
 * <p>
 * In the layered architecture this entity serves as both the persistence
 * model and the domain model — no separate NotificationId value object.
 */
@Entity
@Table(name = "notifications")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "created_at", nullable = false)
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

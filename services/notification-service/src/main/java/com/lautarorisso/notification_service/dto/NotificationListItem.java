package com.lautarorisso.notification_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight REST response DTO for notification list items.
 */
@Schema(description = "Notification list item")
public record NotificationListItem(
        @Schema(description = "Unique identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID id,

        @Schema(description = "Notification type", example = "INCIDENT_ASSIGNED")
        String type,

        @Schema(description = "Notification title", example = "You have been assigned to incident")
        String title,

        @Schema(description = "Read status", example = "UNREAD")
        String status,

        @Schema(description = "Creation timestamp", example = "2026-07-29T12:00:00Z")
        Instant createdAt
) {
}

package com.lautarorisso.notification_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * REST response DTO for a notification.
 */
@Schema(description = "Notification returned in API responses")
public record NotificationResponse(
        @Schema(description = "Unique identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID id,

        @Schema(description = "Notification type", example = "INCIDENT_ASSIGNED")
        String type,

        @Schema(description = "Target user ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID userId,

        @Schema(description = "Related incident ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID incidentId,

        @Schema(description = "Notification title", example = "You have been assigned to incident")
        String title,

        @Schema(description = "Notification message body", example = "Incident #123 has been assigned to you")
        String message,

        @Schema(description = "Delivery status", example = "PENDING")
        String deliveryStatus,

        @Schema(description = "Read status", example = "UNREAD")
        String readStatus,

        @Schema(description = "Creation timestamp", example = "2026-07-29T12:00:00Z")
        Instant createdAt
) {
}

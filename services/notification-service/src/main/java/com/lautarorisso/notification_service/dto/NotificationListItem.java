package com.lautarorisso.notification_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight REST response DTO for notification list items.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification list item")
public class NotificationListItem {

    @Schema(description = "Unique identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;

    @Schema(description = "Notification type", example = "INCIDENT_ASSIGNED")
    private String type;

    @Schema(description = "Notification title", example = "You have been assigned to incident")
    private String title;

    @Schema(description = "Read status", example = "UNREAD")
    private String status;

    @Schema(description = "Creation timestamp", example = "2026-07-29T12:00:00Z")
    private Instant createdAt;
}

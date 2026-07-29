package com.lautarorisso.notification_service.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * REST response DTO for a notification.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification returned in API responses")
public class NotificationResponse {

    @Schema(description = "Unique identifier", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;

    @Schema(description = "Notification type", example = "INCIDENT_ASSIGNED")
    private String type;

    @Schema(description = "Target user ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID userId;

    @Schema(description = "Related incident ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID incidentId;

    @Schema(description = "Notification title", example = "You have been assigned to incident")
    private String title;

    @Schema(description = "Notification message body", example = "Incident #123 has been assigned to you")
    private String message;

    @Schema(description = "Read status", example = "UNREAD")
    private String status;

    @Schema(description = "Creation timestamp", example = "2026-07-29T12:00:00Z")
    private Instant createdAt;
}

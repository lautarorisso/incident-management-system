package com.lautarorisso.notification_service.controller;

import com.lautarorisso.notification_service.dto.NotificationListItem;
import com.lautarorisso.notification_service.dto.NotificationResponse;
import com.lautarorisso.notification_service.entity.Notification;
import com.lautarorisso.notification_service.enums.NotificationReadStatus;
import com.lautarorisso.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for notification read and update operations.
 * <p>
 * Thin HTTP mapping layer: authorization (owner-check), filtering, and
 * persistence live in {@link NotificationService}.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications for a user",
            description = "Returns notifications for the specified user, optionally filtered by read status. "
                    + "Access is restricted to the owner (JWT sub == userId) or an admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of notifications",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationListItem.class)))),
            @ApiResponse(responseCode = "400", description = "Missing required userId parameter or invalid status value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Not the notification owner and not an admin")
    })
    public ResponseEntity<List<NotificationListItem>> getNotifications(
            @Parameter(description = "User ID to get notifications for", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Filter by read status (UNREAD, READ)")
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        NotificationReadStatus readStatus = null;
        if (status != null) {
            readStatus = NotificationReadStatus.valueOf(status.toUpperCase());
        }

        List<Notification> notifications = notificationService.getNotifications(
                jwt, authentication, userId, readStatus);

        return ResponseEntity.ok(toListItemList(notifications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification found",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the notification owner and not an admin"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> getNotificationById(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return notificationService.getById(jwt, authentication, id)
                .map(notification -> ResponseEntity.ok(toResponse(notification)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read",
            description = "Sets the notification read status to READ (delivery status preserved) and returns the updated notification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the notification owner and not an admin"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return notificationService.markAsRead(jwt, authentication, id)
                .map(notification -> ResponseEntity.ok(toResponse(notification)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Manual mapping helpers (no MapStruct) ---

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType() != null ? notification.getType().name() : null,
                notification.getUserId(),
                notification.getIncidentId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getDeliveryStatus() != null ? notification.getDeliveryStatus().name() : null,
                notification.getReadStatus() != null ? notification.getReadStatus().name() : null,
                notification.getCreatedAt());
    }

    private NotificationListItem toListItem(Notification notification) {
        return new NotificationListItem(
                notification.getId(),
                notification.getType() != null ? notification.getType().name() : null,
                notification.getTitle(),
                notification.getDeliveryStatus() != null ? notification.getDeliveryStatus().name() : null,
                notification.getReadStatus() != null ? notification.getReadStatus().name() : null,
                notification.getCreatedAt());
    }

    private List<NotificationListItem> toListItemList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }
}
